package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.scormcloudservice.api.ScormException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GradebookConnection {

    private ScormServiceStore store;
    private GradebookExternalAssessmentService gradebookService;

    private static final Logger LOG = LoggerFactory.getLogger(GradebookConnection.class);
    private static int MAX_TITLE_ATTEMPTS = 100;


    public GradebookConnection(ScormServiceStore store) {
        this.store = store;

        gradebookService = (GradebookExternalAssessmentService)org.sakaiproject.component.cover.ComponentManager.get("org.sakaiproject.service.gradebook.GradebookExternalAssessmentService");
    }


    // THINKME: should we be working with doubles instead of longs?
    public void sendScore(String registrationId, Long scoreFromResult) throws ScormException {
        String userId = store.getUserForRegistration(registrationId);
        ScormCourse course = store.getCourseForRegistration(registrationId);

        createAssessmentIfMissing(course.getSiteId(), course.getId(), course.getTitle());
        gradebookService.updateExternalAssessmentScore(course.getSiteId(), course.getId(), userId, scoreFromResult.toString());
    }


    public void createAssessmentIfMissing(String siteId, String assessmentId, String title)
        throws ScormException {
        boolean succeeded = false;

        for (int attempt = 1; attempt < MAX_TITLE_ATTEMPTS; attempt++) {
            try {
                createAssessmentIfMissing(siteId, assessmentId, title, attempt);
                succeeded = true;
                break;
            } catch (ConflictingAssignmentNameException e) {
            }
        }

        if (!succeeded) {
            throw new ScormException("Couldn't create an assessment with title: " + title + " for site " + siteId);
        }
    }

    private void createAssessmentIfMissing(String siteId, String assessmentId, String title, int attempt) {
        if (!gradebookService.isGradebookDefined(siteId)) {
            LOG.error("Can't update gradebook for site {} because there's no gradebook", siteId);
            return;
        }

        if (attempt > 1) {
            title = (title + " (" + attempt + ")");
        }

        if (gradebookService.isExternalAssignmentDefined(siteId, assessmentId)) {
            // Just update title
            gradebookService.updateExternalAssessment(siteId, assessmentId, null, title, (double)100.0, null);
        } else {
            gradebookService.addExternalAssessment(siteId, assessmentId, null, title, (double)100.0, null, "SCORM Cloud Service");
        }
    }
}

