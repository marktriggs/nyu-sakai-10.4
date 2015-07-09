package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.scormcloudservice.api.ScormCloudService;

import com.rusticisoftware.hostedengine.client.Configuration;
import com.rusticisoftware.hostedengine.client.CourseService;
import com.rusticisoftware.hostedengine.client.ScormCloud;

import org.sakaiproject.component.cover.ServerConfigurationService;


class ScormCloudServiceImpl implements ScormCloudService {

    public String getScormPlayerUrl(String externalId) {
        return "http://www.sakaiproject.org";
    }

    public void addCourse(String siteId, String externalId, String resourceId) throws ScormException {
        new ScormJobStore().add(siteId, externalId, resourceId);
    }

    public void init() {
        Configuration config = getConfiguration();
        ScormCloud.setConfiguration(config);

        try {
            CourseService service = ScormCloud.getCourseService();

            // service
            System.err.println("\n*** DEBUG " + System.currentTimeMillis() + "[ScormCloudServiceImpl.java:26 f6c163]: " + "\n    service => " + (service) + "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy() {
    }


    public Configuration getConfiguration() {
        String appId = ServerConfigurationService.getString("scormcloudservice.appid", "");
        String secret = ServerConfigurationService.getString("scormcloudservice.secret", "");

        if (appId.isEmpty() || secret.isEmpty()) {
            throw new RuntimeException("You need to specify scormcloudservice.appid and scormcloudservice.secret");
        }

        return new Configuration(ServerConfigurationService.getString("scormcloudservice.url", "http://cloud.scorm.com/api"),
                ServerConfigurationService.getString("scormcloudservice.appid", ""),
                ServerConfigurationService.getString("scormcloudservice.secret", ""),
                "sakai.scormcloudservice");
    }

}
