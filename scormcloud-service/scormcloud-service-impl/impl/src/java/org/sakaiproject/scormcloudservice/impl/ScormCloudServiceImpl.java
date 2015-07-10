package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.scormcloudservice.api.ScormCloudService;

import com.rusticisoftware.hostedengine.client.Configuration;
import com.rusticisoftware.hostedengine.client.CourseService;
import com.rusticisoftware.hostedengine.client.RegistrationService;
import com.rusticisoftware.hostedengine.client.ScormCloud;

import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.user.api.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class ScormCloudServiceImpl implements ScormCloudService {

    private static final Logger LOG = LoggerFactory.getLogger(ScormCloudServiceImpl.class);

    public String getScormPlayerUrl(String siteId, String externalId) throws ScormException {
        User currentUser = UserDirectoryService.getCurrentUser();

        String registrationId = addRegistration(siteId, externalId, currentUser.getId(), currentUser.getFirstName(), currentUser.getLastName());

        try {
            RegistrationService registration = ScormCloud.getRegistrationService();

            // FIXME: need a return URL
            return registration.GetLaunchUrl(registrationId, "https://www.nyu.edu/");
        } catch (Exception e) {
            throw new ScormException("Couldn't determine launch URL", e);
        }
    }

    public void addCourse(String siteId, String externalId, String resourceId) throws ScormException {
        ScormServiceStore store = new ScormServiceStore();
        store.addCourse(siteId, externalId, resourceId);
    }

    // Return true if a registration was added.  False if we already had it.
    public String addRegistration(String siteId, String externalId, String userId, String firstName, String lastName)
        throws ScormException {
        ScormServiceStore store = new ScormServiceStore();

        String registrationId = null;

        if ((registrationId = store.hasRegistration(siteId, externalId, userId)) != null) {
            return registrationId;
        }

        try {
            RegistrationService registration = ScormCloud.getRegistrationService();
            registrationId = store.mintId();
            String courseId = store.findCourse(siteId, externalId);

            registration.CreateRegistration(registrationId, courseId, userId, firstName, lastName);
            store.recordRegistration(registrationId, courseId, userId);

            return registrationId;
        } catch (Exception e) {
            throw new ScormException("Failure while creating registration", e);
        }
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
