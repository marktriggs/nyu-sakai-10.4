package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.scormcloudservice.api.ScormCloudService;

import com.rusticisoftware.hostedengine.client.Configuration;
import com.rusticisoftware.hostedengine.client.CourseService;
import com.rusticisoftware.hostedengine.client.ScormCloud;


class ScormCloudServiceImpl implements ScormCloudService {

    public String getScormPlayerUrl(String externalId) {
        return "http://www.sakaiproject.org";
    }

    public void addCourse(String siteId, String externalId, String resourceId) throws ScormException {
        new ScormJobStore().add(siteId, externalId, resourceId);
    }

    public void init() {
        Configuration config = new Configuration("http://thweeble:2345", "poop", "poop", "nyuclasses.10.4");
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

}
