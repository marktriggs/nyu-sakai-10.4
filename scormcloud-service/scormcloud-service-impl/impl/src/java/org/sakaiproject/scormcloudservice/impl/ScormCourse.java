package org.sakaiproject.scormcloudservice.impl;


public class ScormCourse extends ScormCourseData {

    public ScormCourse(String uuid, String siteId, String externalId, String resourceId, String title, boolean graded) {
        super(uuid, siteId, externalId, resourceId, title, graded);
    }

    public String toString() {
        return "#<Course " + getId() + ">";
    }
}

