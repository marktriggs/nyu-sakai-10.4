package org.sakaiproject.scormcloudservice.api;

import javax.servlet.http.HttpServletRequest;

public interface ScormCloudService
{
    public String getScormPlayerUrl(String siteId, String externalId) throws ScormRegistrationNotFoundException, ScormException;

    public void addCourse(String siteId, String externalId, String resourceId) throws ScormException;

    public String addRegistration(String siteId, String externalId, String userId, String firstName, String lastName) throws ScormException;

    public void runProcessingRound() throws ScormException;
}
