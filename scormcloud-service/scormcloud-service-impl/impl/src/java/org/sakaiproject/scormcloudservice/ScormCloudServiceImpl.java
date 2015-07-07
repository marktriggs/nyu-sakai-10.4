package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormCloudService;

class ScormCloudServiceImpl implements ScormCloudService {

    public String getScormPlayerUrl(String externalId) {
        return "http://www.sakaiproject.org";
    }

    public void init() {
    }

    public void destroy() {
    }

}
