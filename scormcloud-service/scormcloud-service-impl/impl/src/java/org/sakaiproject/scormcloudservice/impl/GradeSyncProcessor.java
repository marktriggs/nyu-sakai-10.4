package org.sakaiproject.scormcloudservice.impl;

import com.rusticisoftware.hostedengine.client.ScormCloud;
import com.rusticisoftware.hostedengine.client.datatypes.ImportResult;

import org.sakaiproject.scormcloudservice.api.ScormException;

import com.rusticisoftware.hostedengine.client.Configuration;
import com.rusticisoftware.hostedengine.client.RegistrationService;
import com.rusticisoftware.hostedengine.client.ScormCloud;
import com.rusticisoftware.hostedengine.client.datatypes.RegistrationData;

import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.content.api.ContentResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.TypeException;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.authz.api.SecurityAdvisor;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.component.cover.ComponentManager;

import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

class GradeSyncProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ScormCloudJobProcessor.class);

    private static final String SYNC_USER = "admin";

    // FIXME: sakai.properties
    final int MAX_SCORM_GRADESYNC_THREADS = 1;

    // FIXME: Add connection timeout on the individual scorm requests too
    final int TIMEOUT_SECONDS = 300;

    private ScormServiceStore store;

    public void run() throws ScormException {
        ScormServiceStore store = new ScormServiceStore();

        try {
            Date startTime = new Date();
            CoursesForSync courses = store.getCoursesNeedingSync();
            if (!courses.isEmpty()) {
                syncCourses(courses);
            }
            store.setLastSyncTime(startTime);
        } catch (ScormException e) {
            LOG.error("Failure while getting list of Scorm courses for grade sync", e);
        }
    }


    private void syncCourses(final CoursesForSync courses) throws ScormException {
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(courses.size(), MAX_SCORM_GRADESYNC_THREADS));
        try {
            List<Future<Void>> results = new ArrayList<Future<Void>>();

            // Callable?
            for (final String courseId : courses.getCourseIds()) {
                Future<Void> result = workers.submit(new Callable() {
                    public Void call() {
                        Thread.currentThread().setName("ScormCloudService-GradeSync-" + courseId);

                        SessionManager sessionManager = (SessionManager) ComponentManager.get("org.sakaiproject.tool.api.SessionManager");
                        Session s = sessionManager.startSession();
                        s.setActive();
                        s.setUserId(SYNC_USER);
                        s.setUserEid(SYNC_USER);
                        sessionManager.setCurrentSession(s);

                        try {
                            syncCourse(courseId, courses.getLastSyncTime(), new ScormServiceStore());
                        } catch (ScormException e) {
                            throw new RuntimeException(e);
                        }

                        return null;
                    }
                });

                results.add(result);
            }

            Throwable failure = null;
            for (Future<Void> worker : results) {
                try {
                    worker.get();
                } catch (ExecutionException e) {
                    failure = e;
                } catch (InterruptedException e) {
                    failure = e;
                }
            }

            if (failure != null) {
                throw new ScormException("Failure while syncing courses", failure);
            }

        } finally {
            workers.shutdown();
            try {
                while (!workers.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {}
        }
    }


    private class ScormScore {

        boolean resetRequest = false;
        boolean unknownScore = false;
        String rawScore;
        double score;

        public ScormScore(String s) {
            rawScore = s;

            if (s.equals("unknown")) {
                score = 0.0;
                if (s.equals("reset")) {
                    score = 0.0;
                    resetRequest = true;
                } else {
                    try {
                        this.score = Double.valueOf(s);
                    } catch (NumberFormatException e) {
                        unknownScore = true;
                    }
                }
            }
        }

        public double getScore() {
            if (resetRequest || unknownScore) {
                throw new IllegalStateException("Can't fetch a score from an unknown/reset request");
            }

            return score;
        }

        public boolean isReset() {
            return resetRequest;
        }

        public boolean isUnknown() {
            return unknownScore;
        }

        public String getRawScore() {
            return rawScore;
        }
    }


    private ScormScore extractScore(String xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
        Document parsed = docBuilder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression expr = xpath.compile("//complete");

        if (!"complete".equals((String)expr.evaluate(parsed, XPathConstants.STRING))) {
            return null;
        }

        xpath = xPathfactory.newXPath();
        expr = xpath.compile("//score");

        String value = (String)expr.evaluate(parsed, XPathConstants.STRING);

        return new ScormScore(value);
    }


    private void syncCourse(final String courseId, Date lastCheckTime, final ScormServiceStore store)
        throws ScormException {
        LOG.info("Syncing SCORM courseId: " + courseId);

        GradebookConnection gradebook = new GradebookConnection(store);

        try {
            RegistrationService registrationService = ScormCloud.getRegistrationService();

            List<RegistrationData> registrationList = registrationService.GetRegistrationList(null, null, courseId, null, lastCheckTime, null);

            for (RegistrationData registration : registrationList) {
                String registrationId = registration.getRegistrationId();
                String registrationResult = registrationService.GetRegistrationResult(registrationId);

                ScormScore scoreFromResult = extractScore(registrationResult);

                if (scoreFromResult.isReset()) {
                    store.removeScore(registrationId);
                    gradebook.removeScore(registrationId);
                } else if (scoreFromResult.isUnknown()) {
                    LOG.error("Received an unparseable score from SCORM Cloud API for registration: " + registrationId +
                            " score was: " + scoreFromResult.getRawScore());
                } else {
                    store.recordScore(registrationId, scoreFromResult.getScore());
                    gradebook.sendScore(registrationId, scoreFromResult.getScore());
                }
            }
        } catch (Exception e) {
            throw new ScormException("Failure when syncing grades for " + courseId, e);
        }
    }

}
