package org.sakaiproject.portal.charon.site;

import java.util.Collection;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.portal.api.PortalRenderContext;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;


public class NYUSiteInfoRename {
    public void applySettings(PortalRenderContext rcontext, Site site, String userId) {
        boolean isInstructor = false;

        if (SecurityService.unlock(SiteService.SECURE_UPDATE_SITE, site.getReference())) {
            // NYU: Should we show siteinfo as "settings"?
            isInstructor = true;
            rcontext.put("showSiteInfoAsSettings", "true");
        } else {
            rcontext.put("showSiteInfoAsSettings", "false");
        }

        rcontext.put("showJoinableGroups", "false");

        Collection<Group> userGroups = site.getGroupsWithMember(userId);
        if (!isInstructor && userGroups != null && !userGroups.isEmpty()) {
            // If the user is already in a group, show the tool
            rcontext.put("showJoinableGroups", "true");
        } else {
            // Or if there are joinable groups they may want to join, show it.
            for (Group g : site.getGroups()) {
                if (g.getProperties().getProperty(Group.GROUP_PROP_JOINABLE_SET) != null) {
                    rcontext.put("showJoinableGroups", "true");
                    break;
                }
            }
        }

    }
}
