package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.scormcloudservice.api.ScormCloudService;

import com.rusticisoftware.hostedengine.client.Configuration;
import com.rusticisoftware.hostedengine.client.CourseService;
import com.rusticisoftware.hostedengine.client.ScormCloud;

import org.sakaiproject.component.cover.ServerConfigurationService;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ScormCloudServiceImpl implements ScormCloudService {

    private static final Logger LOG = LoggerFactory.getLogger(ScormCloudServiceImpl.class);

    public String getScormPlayerUrl(String externalId) {
        return "http://www.sakaiproject.org";
    }

    public void addCourse(String siteId, String externalId, String resourceId) throws ScormException {
        ScormJobStore store = new ScormJobStore();
        store.add(siteId, externalId, resourceId);
    }

    public void init() {
        Configuration config = getConfiguration();
        ScormCloud.setConfiguration(config);
    }

    public void destroy() {
    }


    public Configuration getConfiguration() {
        String appId = ServerConfigurationService.getString("scormcloudservice.appid", "");
        String secret = ServerConfigurationService.getString("scormcloudservice.secret", "");

        if (appId.isEmpty() || secret.isEmpty()) {
            throw new RuntimeException("You need to specify scormcloudservice.appid and scormcloudservice.secret");
        }

        return new Configuration(ServerConfigurationService.getString("scormcloudservice.url", "https://cloud.scorm.com"),
                ServerConfigurationService.getString("scormcloudservice.appid", ""),
                ServerConfigurationService.getString("scormcloudservice.secret", ""),
                "sakai.scormcloudservice");
    }


    public void runProcessingRound() throws ScormException {
        new ScormCloudJobProcessor().run();
    }

}
