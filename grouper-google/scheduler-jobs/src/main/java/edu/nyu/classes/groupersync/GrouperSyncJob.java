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


public class GrouperSyncJob implements StatefulJob {
    private SiteState siteState;
    private SqlService sqlService;
    private CourseManagementService courseManagement;

    private static final Log log = LogFactory.getLog(GrouperSyncJob.class);

    public void setSqlService(SqlService sqlService) {
        this.sqlService = sqlService;
    }

    public void setCourseManagement(CourseManagementService cms) {
        this.courseManagement = cms;
    }

    static <T> Set<T> setSubtract(Collection<T> set1, Collection<T> set2) {
        Set<T> result = new HashSet(set1);
        result.removeAll(set2);

        return result;
    }

    private void syncGroups(Collection<SyncableGroup> groups) {
        // compare memberships against what we have stored
        GrouperSyncStorage storage = GrouperSyncStorage.getInstance();

        for (SyncableGroup syncableGroup : groups) {
            log.info("Syncing group: " + syncableGroup.getTitle() + "(" + syncableGroup.getId() + ")");

            Set<String> userIds = storage.getUserIds(syncableGroup.getId());
            Set<String> membershipUserIds = syncableGroup.getUserIds();

            Set<String> addedUsers = setSubtract(membershipUserIds, userIds);
            Set<String> droppedUsers = setSubtract(userIds, membershipUserIds);

            log.info("Added users: " + addedUsers);
            log.info("Dropped users: " + droppedUsers);
            
            Set<UserWithRole> changedRoles = setSubtract(syncableGroup.getMembers(), storage.getMembers(syncableGroup.getId()));
            Iterator<UserWithRole> iterator = changedRoles.iterator();
            while (iterator.hasNext()) {
                UserWithRole membership = iterator.next();

                if (addedUsers.contains(membership.getUsername())) {
                    iterator.remove();
                }
            }

            log.info("Changed roles: " + changedRoles);
            
            storage.recordMembers(syncableGroup);
        }

    }

    protected void syncGroups(UpdatedSite update) {
        try {
            log.info("Syncing groups for site: " + update.getSiteId());

            SiteGroupReader groupReader = new SiteGroupReader(update.getSiteId(), courseManagement);
            Collection<SyncableGroup> siteGroups = groupReader.groups();

            syncGroups(siteGroups);
        } catch (IdUnusedException e) {
            log.warn("Couldn't find site: " + update.getSiteId());
        }
    }

    public void init() {
        log.info("GrouperSyncJob initialized");
        siteState = new SiteState(sqlService);
    }

    public void execute(JobExecutionContext context) throws JobExecutionException {
        Set<String> processedSites = new HashSet<String>();

        for (UpdatedSite update : siteState.findUpdatedSites()) {
            if (processedSites.contains(update.getSiteId())) {
                // Already processed during this round.
                continue;
            }

            log.debug(System.currentTimeMillis() + ": Syncing site: " + update.getSiteId());
            syncGroups(update);
            log.debug(System.currentTimeMillis() + ": Completed: " + update.getSiteId());

            processedSites.add(update.getSiteId());
        }
    }
}
