package org.sakaiproject.scormcloudservice.impl;


public class ScormJob {
    private String uuid;
    private String siteId;
    private String externalId;
    private String resourceId;

    public ScormJob(String uuid, String siteId, String externalId, String resourceId) {
        this.uuid = uuid;
        this.siteId = siteId;
        this.externalId = externalId;
        this.resourceId = resourceId;
    }

    public String getId() {
        return uuid;
    }

    public String getSiteId() {
        return siteId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String toString() {
        return "#<Job " + getId() + ">";
    }
}

