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


class GradeSyncProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ScormCloudJobProcessor.class);

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

        for (final String courseId : courses.getCourseIds()) {
            workers.execute(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("ScormCloudService-GradeSync-" + courseId);
                    syncCourse(courseId, courses.getLastSyncTime(), new ScormServiceStore());
                }
            });
        }

        workers.shutdown();
        try {
            while (!workers.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {}
    }



    private Long extractScore(String xml) throws Exception {
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

        if ("unknown".equals(value)) {
            return 0l;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private void syncCourse(final String courseId, Date lastCheckTime, final ScormServiceStore store) {
        try {
            LOG.info("Syncing SCORM courseId: " + courseId);

            try {
                RegistrationService registrationService = ScormCloud.getRegistrationService();

                List<RegistrationData> registrationList = registrationService.GetRegistrationList(null, null, courseId, null, lastCheckTime, null);

                for (RegistrationData registration : registrationList) {
                    String registrationId = registration.getRegistrationId();
                    String registrationResult = registrationService.GetRegistrationResult(registrationId);

                    Long scoreFromResult = extractScore(registrationResult);

                    if (scoreFromResult != null) {
                        store.recordScore(registrationId, scoreFromResult);
                    }
                }
            } catch (Exception e) {
                throw new ScormException("Failure when syncing grades for " + courseId, e);
            }
        } catch (Exception e) {
            LOG.error("Failure while syncing grades for courseId " + courseId, e);
        }
    }

}
