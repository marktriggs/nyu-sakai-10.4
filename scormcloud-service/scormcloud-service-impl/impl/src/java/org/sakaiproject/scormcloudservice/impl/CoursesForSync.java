package org.sakaiproject.scormcloudservice.impl;

import java.util.Date;
import java.util.List;

public class CoursesForSync {

    private List<String> courseIds;
    private Date lastSyncTime;


    public CoursesForSync(List<String> courseIds, long lastSyncTime) {
        this.courseIds = courseIds;
        this.lastSyncTime = new Date(lastSyncTime);
    }


    public boolean isEmpty() {
        return courseIds.isEmpty();
    }

    public int size() {
        return courseIds.size();
    }

    public List<String> getCourseIds() {
        return courseIds;
    }

    public Date getLastSyncTime() {
        return lastSyncTime;
    }

}

