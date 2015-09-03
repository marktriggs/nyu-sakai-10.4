package edu.nyu.classes.groupersync;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.sakaiproject.db.api.SqlService;

import org.quartz.StatefulJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.exception.IdUnusedException;

import org.sakaiproject.coursemanagement.api.CourseManagementService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.ParseException;
import org.apache.commons.logging.Log;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.sakaiproject.api.app.scheduler.JobBeanWrapper;
import org.sakaiproject.api.app.scheduler.SchedulerManager;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;


public class GrouperSyncJob implements StatefulJob {
    private static final Log log = LogFactory.getLog(GrouperSyncJob.class);

    private void syncGroups(Collection<SyncableGroup> groups, GrouperSyncStorage storage) {
        for (SyncableGroup syncableGroup : groups) {
            try {
                String sakaiGroupId = syncableGroup.getId();
                String grouperGroupId = storage.getGrouperIdForSakaiGroup(sakaiGroupId);

                if (grouperGroupId == null) {
                    // This group hasn't been marked as needing syncing.
                    continue;
                }

                log.info("Syncing group: " + syncableGroup.getTitle() + "(" + grouperGroupId + ")");

                Collection<UserWithRole> formerMembers = storage.getMembers(grouperGroupId);
                Collection<UserWithRole> currentMembers = syncableGroup.getMembers();

                log.info("Former members: " + formerMembers);
                log.info("Current members: " + currentMembers);

                Sets.KeyFn byUsername = new Sets.KeyFn<UserWithRole>() {
                    public Object key(UserWithRole user) {
                        return user.getUsername();
                    }
                };

                Set<UserWithRole> addedUsers = Sets.subtract(currentMembers, formerMembers, byUsername);
                Set<UserWithRole> droppedUsers = Sets.subtract(formerMembers, currentMembers, byUsername);
                Set<UserWithRole> changedRoles = Sets.subtract(Sets.subtract(currentMembers, formerMembers),
                                                               addedUsers);

                log.info("Added users: " + addedUsers);
                log.info("Dropped users: " + droppedUsers);
                log.info("Changed roles: " + changedRoles);
            
                storage.recordChanges(grouperGroupId, addedUsers, droppedUsers, changedRoles);
            } catch (GrouperSyncException e) {
                log.error("Hit an error while syncing Sakai group: " + syncableGroup.getId(), e);
            }
        }
    }

    protected void syncGroups(UpdatedSite update, GrouperSyncStorage storage, CourseManagementService courseManagement) {
        try {
            log.info("Syncing groups for site: " + update.getSiteId());

            SiteGroupReader groupReader = new SiteGroupReader(update.getSiteId(), courseManagement);
            Collection<SyncableGroup> siteGroups = groupReader.groups();

            syncGroups(siteGroups, storage);
        } catch (IdUnusedException e) {
            log.warn("Couldn't find site: " + update.getSiteId());
        }
    }

    public void init() {
        log.info("GrouperSyncJob initializing");

        SchedulerManager schedulerManager = (SchedulerManager) ComponentManager.get("org.sakaiproject.api.app.scheduler.SchedulerManager");
        Scheduler scheduler = schedulerManager.getScheduler();

        try {
            if (!ServerConfigurationService.getBoolean("startScheduler@org.sakaiproject.api.app.scheduler.SchedulerManager", true)) {
                log.info("Doing nothing because the scheduler isn't started");
                return;
            }

            registerQuartzJob(scheduler, "GrouperSyncJob", GrouperSyncJob.class, ServerConfigurationService.getString("grouper-google.import-job-cron", "0 * * * * ?"));
        } catch (SchedulerException e) {
            log.error("Error while scheduling Grouper sync job", e);
        } catch (ParseException e) {
            log.error("Parse error when parsing cron expression", e);
        }
    }

    private void registerQuartzJob(Scheduler scheduler, String jobName, Class className, String cronTrigger)
        throws SchedulerException, ParseException {
        // Delete any old instances of the job
        scheduler.deleteJob(jobName, jobName);

        JobDetail detail = new JobDetail(jobName, jobName, className, false, false, false);

        detail.getJobDataMap().put(JobBeanWrapper.SPRING_BEAN_NAME, this.getClass().toString());

        Trigger trigger = new CronTrigger(jobName + "Trigger", jobName, cronTrigger);

        scheduler.scheduleJob(detail, trigger);

        log.info("Scheduled Grouper Sync job: " + jobName);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            SqlService sqlService = (SqlService)ComponentManager.get("org.sakaiproject.db.api.SqlService");
            CourseManagementService cms = (CourseManagementService)ComponentManager.get("org.sakaiproject.coursemanagement.api.CourseManagementService");

            if (sqlService == null || cms == null) {
                throw new GrouperSyncException("Required dependencies were missing!");
            }


            Set<String> processedSites = new HashSet<String>();
            UpdatedSites updatedSites = new UpdatedSites(sqlService);
            GrouperSyncStorage storage = new GrouperSyncStorage();

            storage.prepopulateGroupsBasedOnThisOneWeirdTrick();

            Date now = new Date();
            Date previousTime = storage.getLastRunDate();

            log.info("Running GrouperSyncJob (last run time was " + previousTime + ")");

            for (UpdatedSite update : updatedSites.listSince(previousTime)) {
                if (processedSites.contains(update.getSiteId())) {
                    // Already processed during this round.
                    continue;
                }

                log.info("Syncing site: " + update.getSiteId());
                syncGroups(update, storage, cms);
                log.info("Syncing site completed: " + update.getSiteId());

                processedSites.add(update.getSiteId());
            }

            storage.setLastRunDate(now);

            log.info("GrouperSyncJob completed");
        } catch (GrouperSyncException e) {
            log.error("Failure during job run", e);
            throw new JobExecutionException(e);
        }
    }
}
