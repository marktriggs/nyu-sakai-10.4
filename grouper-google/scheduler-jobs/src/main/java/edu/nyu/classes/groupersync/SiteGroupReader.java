package edu.nyu.classes.groupersync;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.site.api.Group;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.authz.api.Member;
import java.util.HashSet;
import java.util.List;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.authz.api.AuthzGroup;

class SiteGroupReader {

    private String siteId;
    private CourseManagementService cms;

    public SiteGroupReader(String siteId, CourseManagementService cms) {
        this.siteId = siteId;
        this.cms = cms;
    }

    public Collection<SyncableGroup> groups() throws IdUnusedException {
        Collection<SyncableGroup> result = new ArrayList<SyncableGroup>();

        result.addAll(readSakaiGroups());

        return result;
    }

    private Collection<SyncableGroup> readSakaiGroups() throws IdUnusedException {
        Collection<SyncableGroup> result = new ArrayList<SyncableGroup>();

        Collection<AuthzGroup> sakaiGroups = new ArrayList<AuthzGroup>();
        Site site = SiteService.getSite(siteId);
        sakaiGroups.add(site);
        sakaiGroups.addAll(site.getGroups());

        for (AuthzGroup sakaiGroup : sakaiGroups) {
            List<UserWithRole> members = new ArrayList<UserWithRole>();
            HashSet<String> inactiveUsers = new HashSet<String>();
        
            // Load direct members of this group
            for (Member m : sakaiGroup.getMembers()) {
                if (!m.isActive()) {
                    inactiveUsers.add(m.getUserEid());
                    continue;
                }

                if (!m.isProvided()) {
                    // Provided users will be handled separately below.
                    members.add(new UserWithRole(m.getUserEid(), m.getRole().getId()));
                }
            }

            String provider = sakaiGroup.getProviderGroupId();

            // Plus those provided by sections
            if (provider != null) {
                HashSet<String> seenUsers = new HashSet<String>();
                
                for (String providerId : provider.split("\\+")) {
                    for (org.sakaiproject.coursemanagement.api.Membership m : cms.getSectionMemberships(providerId)) {
                        if (seenUsers.contains(m.getUserId()) || inactiveUsers.contains(m.getUserId())) {
                            continue;
                        }

                        members.add(new UserWithRole(m.getUserId(), m.getRole()));
                        seenUsers.add(m.getUserId());
                    }
                }
            }

            result.add(new SyncableGroup(sakaiGroup.getId(), sakaiGroup.getDescription(), members));
        }

        return result;
    }
}
