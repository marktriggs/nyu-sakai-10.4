package org.sakaiproject.scormcloudservice.impl;


public class ScormJob extends ScormCourseData {

    public ScormJob(String uuid, String siteId, String externalId, String resourceId, String title, boolean graded) {
        super(uuid, siteId, externalId, resourceId, title, graded);
    }

    public String toString() {
        return "#<Job " + getId() + ">";
    }
}

