// FIXME: how do we treat inactive as a delete?


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


abstract class Memberships
{
    public abstract Collection<Membership> getMembers();

    public abstract String getId();
    public abstract String getTitle();


    public Set<String> getUserIds() {
        Set<String> result = new HashSet<String>();

        for (Membership m : getMembers()) {
            result.add(m.getUsername());
        }

        return result;
    }
}


class UpdatedSite
{
    private Date updateTime;
    private String siteId;


    public UpdatedSite(String siteId, Date updateTime)
    {
        this.siteId = siteId;
        this.updateTime = updateTime;
    }


    public String getSiteId() {
        return siteId;
    }
}


class SiteState
{
    private SqlService sqlService;
    private static final Log log = LogFactory.getLog(SiteState.class);


    public SiteState(SqlService sqlService)
    {
        this.sqlService = sqlService;
    }


    private String extractSiteId(String s) {
        Matcher m = Pattern.compile("/site/([^/]*)/?").matcher(s);
        if (m.find()) {
            return m.group(1);
        } else {
            return s;
        }
    }


    private void addUpdatedSites(Connection db, String selectColumn, String table,
                                 Timestamp since, String where,
                                 List<UpdatedSite> result)
        throws SQLException
    {
        String sql = "select %s, modifiedon from %s where modifiedon >= ? AND %s";

        PreparedStatement ps = db.prepareStatement(String.format(sql, selectColumn, table, where));

        ps.setTimestamp(1, since);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            String siteId = extractSiteId(rs.getString(1));

            if (siteId != null) {
                result.add(new UpdatedSite(siteId, rs.getTimestamp(2)));
            }
        }

        rs.close();
        ps.close();
    }


    public List<UpdatedSite> findUpdatedSites()
    {
        log.debug(System.currentTimeMillis() + ": Scanning for updated sites");
        List<UpdatedSite> result = new ArrayList<UpdatedSite>();

        // FIXME: Timestamp since = new Timestamp(new Date().getTime() - (1 * 60 * 60 * 1000));
        Timestamp since = new Timestamp(new Date().getTime() - (10 * 1000));

        Connection db = null;
        try {
            db = sqlService.borrowConnection();
            addUpdatedSites(db, "realm_id", "sakai_realm", since, "realm_id like '/site/%'", result);
            addUpdatedSites(db, "site_id", "sakai_site", since, "1 = 1", result);
        } catch (SQLException e) {
            throw new RuntimeException("DB error when looking for updated sites: " + e, e);
        } finally {
            if (db != null) {
                sqlService.returnConnection(db);
            }
        }

        log.debug(System.currentTimeMillis() + ": Completed");

        return result;
    }


    public void markAsProcessed(List<UpdatedSite> sites)
    {
    }
}



class Membership
{
    private String username;
    private String role;


    public Membership(String username, String role)
    {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String toString()
    {
        return String.format("#<%s [%s]>", username, role);
    }

    public String hashKey() {
        return username + "_" + role;
    }

    public int hashCode() {
        return hashKey().hashCode();
    }

    public boolean equals(Object other) {
        if (!(other instanceof Membership)) {
            return false;
        }

        return ((Membership) other).hashKey().equals(hashKey());
    }
}


class GroupOrSection extends Memberships
{
    private String id;
    private String title;
    private String provider;
    private List<Membership> members;

    private CourseManagementService courseManagement;

    public GroupOrSection(Group sakaiGroup, CourseManagementService courseManagement)
    {
        id = sakaiGroup.getId();
        title = sakaiGroup.getTitle();
        provider = sakaiGroup.getProviderGroupId();
        this.courseManagement = courseManagement;

        members = loadMembers(sakaiGroup);
    }

    @Override
    public Collection<Membership> getMembers() {
        return members;
    }


    @Override
    public String getId() {
        return id;
    }


    @Override
    public String getTitle() {
        return title;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\n[");
        sb.append("ID: " + id + "; ");
        sb.append("Title: " + title + "; ");
        sb.append("Provider: " + provider + "; ");
        sb.append("Members:\n");

        for (Membership m : members) {
            sb.append("\n  " + m);
        }

        sb.append("\n]\n");

        return sb.toString();
    }


    private List<Membership> loadMembers(Group sakaiGroup)
    {
        List<Membership> result = new ArrayList<Membership>();
        HashSet<String> seenUsers = new HashSet<String>();
        
        // Load direct members
        for (Member m : sakaiGroup.getMembers()) {
            if (m.isActive()) {
                result.add(new Membership(m.getUserEid(), m.getRole().getId()));
            }

            seenUsers.add(m.getUserEid());
        }

        // Plus those provided by sections
        if (provider != null) {
            for (String providerId : provider.split("\\+")) {
                for (org.sakaiproject.coursemanagement.api.Membership m : courseManagement.getSectionMemberships(providerId)) {
                    if (!seenUsers.contains(m.getUserId())) {
                        result.add(new Membership(m.getUserId(),
                                                            m.getRole()));
                        seenUsers.add(m.getUserId());
                    }
                }
            }
        }

        return result;
    }
}


class GroupReader
{
    private String siteId;
    private CourseManagementService courseManagement;

    public GroupReader(String siteId, CourseManagementService courseManagement)
    {
        this.siteId = siteId;
        this.courseManagement = courseManagement;
    }


    public List<GroupOrSection> groups() throws IdUnusedException
    {
        List<GroupOrSection> result = new ArrayList<GroupOrSection>();
        Collection<Group> sakaiGroups = SiteService.getSite(siteId).getGroups();

        for (Group group : sakaiGroups) {
            result.add(new GroupOrSection(group, courseManagement));
        }

        return result;
    }
}


class AllSiteMembers extends Memberships {

    private Collection<Membership> siteMembers;
    private String id;

    public AllSiteMembers(UpdatedSite site, GroupReader groupReader) throws IdUnusedException {
        id = site.getSiteId();
        siteMembers = new HashSet<Membership>();

        for (GroupOrSection group : groupReader.groups()) {
            siteMembers.addAll(group.getMembers());
        }

        String siteId = site.getSiteId();
        for (Member member : SiteService.getSite(siteId).getMembers()) {
            if (member.isActive()) {
                siteMembers.add(new Membership(member.getUserEid(), member.getRole().getId()));
            }
        }

        dedupeByUserId(siteMembers);
    }


    private void dedupeByUserId(Collection<Membership> members) {
        Set<String> seenUserIds = new HashSet<String>();

        Iterator<Membership> memberships = members.iterator();
        while (memberships.hasNext()) {
            Membership m = memberships.next();
            if (seenUserIds.contains(m.getUsername())) {
                memberships.remove();
            }

            seenUserIds.add(m.getUsername());
        }
    }


    @Override
    public Collection<Membership> getMembers() {
        return siteMembers;
    }


    @Override
    public String getId() {
        return id;
    }


    @Override
    public String getTitle() {
        return "All members";
    }
}


public class GrouperSyncJob implements StatefulJob
{
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


    private void syncMemberships(Collection<Memberships> memberships) {
        // compare memberships against what we have stored

        GrouperSyncStorage storage = GrouperSyncStorage.getInstance();

        for (Memberships m : memberships) {
            Set<String> userIds = storage.getUserIds(m.getId());
            Set<String> membershipUserIds = m.getUserIds();

            Set<String> addedUsers = setSubtract(membershipUserIds, userIds);
            Set<String> droppedUsers = setSubtract(userIds, membershipUserIds);

            // addedUsers, droppedUsers
            System.err.println("\n*** DEBUG " + System.currentTimeMillis() + "[GrouperSyncJob.java:396 db1266]: " + "\n    addedUsers => " + (addedUsers) + "\n    droppedUsers => " + (droppedUsers) + "\n");
            
            Set<Membership> changedRoles = setSubtract(m.getMembers(), storage.getMembers(m.getId()));
            Iterator<Membership> iterator = changedRoles.iterator();
            while (iterator.hasNext()) {
                Membership membership = iterator.next();

                if (addedUsers.contains(membership.getUsername())) {
                    iterator.remove();
                }
            }

            // changedRoles
            System.err.println("\n*** DEBUG " + System.currentTimeMillis() + "[GrouperSyncJob.java:419 a0b11c]: " + "\n    changedRoles => " + (changedRoles) + "\n");
            
            storage.recordMemberships(m);

            // FIXME: Updating an existing section (attached to a site) wasn't detected.  We need to look at SAKAI_REALM_PROVIDER or whatever it's called.

           // 
            // Set<Membership m>storage.getUsersAndRole(m.getId())
        }


    }


    protected void syncGroups(UpdatedSite update)
    {
        try {
            log.info("Groups for site: " + update.getSiteId());

            GroupReader groupsAndSections = new GroupReader(update.getSiteId(), courseManagement);
            AllSiteMembers allSiteMembers = new AllSiteMembers(update, groupsAndSections);

            log.info(groupsAndSections.groups());
            log.info(allSiteMembers.getMembers());

            List<Memberships> siteGroups = new ArrayList<Memberships>();
            siteGroups.addAll(groupsAndSections.groups());
            siteGroups.add(allSiteMembers);

            // Do something with siteGroups
            syncMemberships(siteGroups);
        } catch (IdUnusedException e) {
            log.warn("Couldn't find site: " + update.getSiteId());
        }
    }


    public void init()
    {
        log.info("GrouperSyncJob initialized");
        siteState = new SiteState(sqlService);
    }


    public void execute(JobExecutionContext context) throws JobExecutionException
    {
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
