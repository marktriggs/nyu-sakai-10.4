package org.sakaiproject.scormcloudservice.impl;

import com.rusticisoftware.hostedengine.client.CourseService;
import com.rusticisoftware.hostedengine.client.ScormCloud;
import com.rusticisoftware.hostedengine.client.datatypes.ImportResult;
import org.sakaiproject.authz.api.SecurityAdvisor;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.cover.ContentHostingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.ServerOverloadException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.scormcloudservice.api.ScormException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

class ScormCloudJobProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ScormCloudJobProcessor.class);

    // FIXME: sakai.properties
    final int MAX_SCORM_THREADS = 5;

    // FIXME: Add connection timeout on the individual scorm requests too
    final int TIMEOUT_SECONDS = 300;

    private ScormServiceStore store;

    public void run() throws ScormException {
        ScormServiceStore store = new ScormServiceStore();
        List<ScormJob> jobs = Collections.emptyList();

        try {
            jobs = store.getPendingJobs();
        } catch (ScormException e) {
            LOG.error("Failure while getting list of Scorm jobs for processing", e);
        }

        if (!jobs.isEmpty()) {
            handleJobs(jobs);
        }
    }


    private void handleJobs(List<ScormJob> jobs) {
        ExecutorService workers = Executors.newFixedThreadPool(Math.min(jobs.size(), MAX_SCORM_THREADS));

        for (final ScormJob job : jobs) {
            workers.execute(new Runnable() {
                public void run() {
                    Thread.currentThread().setName("ScormCloudService-Job-" + job.getId());
                    handleJob(job, new ScormServiceStore());
                }
            });
        }

        workers.shutdown();
        try {
            while (!workers.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
        }
    }


    private ContentResource getResource(final String resourceId)
            throws PermissionException, ServerOverloadException, IdUnusedException, TypeException {

        SecurityService.pushAdvisor(new SecurityAdvisor() {
            public SecurityAdvice isAllowed(String userId, String function, String reference) {
                if (("/content" + resourceId).equals(reference)) {
                    return SecurityAdvice.ALLOWED;
                } else {
                    return SecurityAdvice.PASS;
                }
            }
        });

        try {
            return ContentHostingService.getResource(resourceId);
        } finally {
            SecurityService.popAdvisor();
        }
    }

    private File spoolResourceToFile(final ScormJob job)
            throws IOException, PermissionException, ServerOverloadException, IdUnusedException, TypeException {

        ContentResource resource = getResource(job.getResourceId());

        File tmpfile = File.createTempFile("SCORM_Job_" + job.getId(), ".zip");

        InputStream in = null;
        FileOutputStream out = null;

        try {
            in = resource.streamContent();
            out = new FileOutputStream(tmpfile);

            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) >= 0) {
                out.write(buf, 0, len);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }

        return tmpfile;
    }


    private void handleJob(final ScormJob job, final ScormServiceStore store) {
        try {
            LOG.info("Job started");

            // Annoying to have to do this, but the API wants a path, not an InputStream
            File resource = spoolResourceToFile(job);
            try {
                CourseService service = ScormCloud.getCourseService();

                // "Starting import now"
                System.err.println("\n*** DEBUG " + System.currentTimeMillis() + "[ScormCloudJobProcessor.java:137 262ad4]: " + "\n    'Starting import now' => " + ("Starting import now") + "\n");

                List<ImportResult> result = service.ImportCourse(job.getId(), resource.getPath());

                // result
                System.err.println("\n*** DEBUG " + System.currentTimeMillis() + "[ScormCloudJobProcessor.java:137 f369a0]: " + "\n    result => " + (result) + "\n");

                HashMap<String, String> attributes = service.GetAttributes(job.getId());
                // Launch inline
                attributes.put("playerLaunchType", "1");
                attributes.put("scoLaunchType", "1");
                service.UpdateAttributes(job.getId(), attributes);

            } finally {
                resource.delete();
            }

            LOG.info("Processing finished for job");
            store.markCompleted(job);
        } catch (Exception e) {
            LOG.error("Failure while processing SCORM job " + job.getId(), e);
            try {
                store.recordFailure(job);
            } catch (Exception e2) {
                LOG.error("Further failure while marking SCORM job " + job.getId() + " as failed!", e);
            }
        }
    }

}
