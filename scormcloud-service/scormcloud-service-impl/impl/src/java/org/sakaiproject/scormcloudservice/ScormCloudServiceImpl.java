package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.scormcloudservice.api.ScormCloudService;

class ScormCloudServiceImpl implements ScormCloudService {

    public String getScormPlayerUrl(String externalId) {
        return "http://www.sakaiproject.org";
    }

    public void addCourse(String siteId, String externalId, String resourceId) throws ScormException {
        new ScormJobStore().add(siteId, externalId, resourceId);
    }

    public void init() {
    }

    public void destroy() {
    }

}
