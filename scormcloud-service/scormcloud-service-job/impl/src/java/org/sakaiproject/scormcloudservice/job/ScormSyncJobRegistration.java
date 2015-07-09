package org.sakaiproject.scormcloudservice.job;

import java.text.ParseException;

import org.quartz.Scheduler;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.CronTrigger;
import org.quartz.SchedulerException;

import org.sakaiproject.api.app.scheduler.JobBeanWrapper;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.api.app.scheduler.SchedulerManager;
import org.sakaiproject.component.cover.ComponentManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ScormSyncJobRegistration {

    private static final Logger LOG = LoggerFactory.getLogger(ScormSyncJobRegistration.class);

    String JOB_NAME = "ScormSyncJob";
    String JOB_GROUP = "ScormSyncJob";

    public void init()
    {
        SchedulerManager schedulerManager = (SchedulerManager) ComponentManager.get("org.sakaiproject.api.app.scheduler.SchedulerManager");

        Scheduler scheduler = schedulerManager.getScheduler();

        try {
            if (!scheduler.isStarted()) {
                LOG.info("Doing nothing because the scheduler isn't started");
                return;
            }

            // Delete any old instances of the job
            scheduler.deleteJob(JOB_NAME, JOB_GROUP);

            // Then reschedule it
            String cronTrigger = ServerConfigurationService.getString("scormcloudservice.sync-job-cron", "0 * * * * ?");

            JobDetail detail = new JobDetail(JOB_NAME, JOB_GROUP, ScormSyncJob.class, false, false, false);

            detail.getJobDataMap().put(JobBeanWrapper.SPRING_BEAN_NAME, this.getClass().toString());

            Trigger trigger = new CronTrigger("ScormSyncJobTrigger", "ScormSyncJob", cronTrigger);

            scheduler.scheduleJob(detail, trigger);

            LOG.info("Scheduled Scorm Cloud Service sync job!");

        } catch (SchedulerException e) {
            LOG.error("Error while scheduling Scorm Cloud Service sync job", e);
        } catch (ParseException e) {
            LOG.error("Parse error when parsing cron expression", e);
        }
    }

    public void destroy()
    {
    }
}
