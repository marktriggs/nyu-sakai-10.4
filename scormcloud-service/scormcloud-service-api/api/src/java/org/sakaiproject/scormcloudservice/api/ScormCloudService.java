package org.sakaiproject.scormcloudservice.api;

import javax.servlet.http.HttpServletRequest;

public interface ScormCloudService
{
    public String getScormPlayerUrl(String externalId) throws ScormRegistrationNotFoundException, ScormException;
}
