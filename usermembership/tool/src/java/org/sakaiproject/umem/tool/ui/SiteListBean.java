/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/ufp/usermembership/trunk/tool/src/java/org/sakaiproject/umem/tool/ui/SiteListBean.java $
 * $Id: SiteListBean.java 4381 2007-03-21 11:25:54Z nuno@ufp.pt $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.opensource.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.umem.tool.ui;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.SiteService.SelectionType;
import org.sakaiproject.site.api.SiteService.SortType;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.umem.api.Authz;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;



/**
 * @author <a href="mailto:nuno@ufp.pt">Nuno Fernandes</a>
 */
public class SiteListBean {
	private static final long			serialVersionUID	= 2L;
	private static final String			SORT_SITE_NAME		= "siteName";
	private static final String			SORT_GROUPS_TYPE	= "groups";
	private static final String			SORT_SITE_TYPE		= "siteType";
	private static final String			SORT_SITE_RID		= "roleId";
	private static final String			SORT_SITE_PV		= "published";
	private static final String			SORT_USER_STATUS	= "userStatus";
	private static final String			SORT_SITE_TERM		= "siteTerm";
	/** Our log (commons). */
	private static Log					LOG					= LogFactory.getLog(SiteListBean.class);
	/** Resource bundle */
	private transient ResourceLoader	msgs				= new ResourceLoader("org.sakaiproject.umem.tool.bundle.Messages");
	/** Controller fields */
	private List						userSitesRows;
	/** Getter vars */
	private boolean						refreshQuery		= false;
	private boolean						allowed				= false;
	private String						thisUserId			= null;
	private String						userId				= null;
	private boolean						sitesSortAscending	= true;
	private String						sitesSortColumn		= SORT_SITE_NAME;
	/** Resource properties */
	private final static String 		PROP_SITE_TERM 		= "term";
	/** Sakai APIs */
	private SessionManager				M_session			= (SessionManager) ComponentManager.get(SessionManager.class.getName());
	private SqlService					M_sql				= (SqlService) ComponentManager.get(SqlService.class.getName());
	private SiteService					M_site				= (SiteService) ComponentManager.get(SiteService.class.getName());
	private ToolManager					M_tm				= (ToolManager) ComponentManager.get(ToolManager.class.getName());
	private Authz						authz				= (Authz) ComponentManager.get(Authz.class.getName());
	private ServerConfigurationService			M_scf				= (ServerConfigurationService) ComponentManager.get(ServerConfigurationService.class.getName());
	/** Private vars */
	private RuleBasedCollator					collator;
	private long						timeSpentInGroups	= 0;
	private String						portalURL			= M_scf.getPortalUrl();
	private String						message				= "";

	// ######################################################################################
	// UserSitesRow CLASS
	// ######################################################################################

	public class UserSitesRow implements Serializable {
		private static final long	serialVersionUID	= 1L;
		private Site				site;
		private String				siteId;
		private String				siteTitle;
		private String				siteType;
		private String				siteURL;
		private String				groups;
		private String				roleName;
		private String				pubView;
		private String				userStatus;
		private String				siteTerm;

		{
			try{
				collator= new RuleBasedCollator(((RuleBasedCollator)Collator.getInstance()).getRules().replaceAll("<'\u005f'", "<' '<'\u005f'"));
			}catch(ParseException e){
				collator = (RuleBasedCollator)Collator.getInstance();
			}
		}
		public UserSitesRow() {
		}

		public UserSitesRow(String siteId, String siteTitle, String siteType, String groups, String roleName, String pubView, String userStatus, String term) {
			this.siteId = siteId;
			this.siteTitle = siteTitle;
			this.siteType = siteType;
			this.groups = groups;
			this.roleName = roleName;
			this.pubView = pubView;
			this.userStatus = userStatus;
			this.siteTerm = term;
		}

		public UserSitesRow(Site site, String groups, String roleName) {
			this.siteId = site.getId();
			this.siteTitle = site.getTitle();
			this.siteType = site.getType();
			this.groups = groups;
			this.roleName = roleName;
			this.pubView = site.isPublished() ? msgs.getString("status_published") : msgs.getString("status_unpublished");
			this.userStatus = site.getMember(userId).isActive() ? msgs.getString("site_user_status_active") : msgs.getString("site_user_status_inactive");
			this.siteTerm = site.getProperties().getProperty(PROP_SITE_TERM);
		}

		public String getSiteId() {
			return siteId;
		}

		public String getSiteTitle() {
			return siteTitle;
		}

		public String getSiteType() {
			return siteType;
		}

		public String getSiteURL() {
			StringBuilder siteUrl = new StringBuilder();
			siteUrl.append(portalURL);
			siteUrl.append("/site/");
			siteUrl.append(siteId);
			return siteUrl.toString();
		}

		public String getGroups() {
			return groups;
		}

		public String getRoleName() {
			return roleName;
		}

		public String getPubView() {
			return pubView;
		}
		
		public String getUserStatus(){
			return this.userStatus;
		}

		public String getSiteTerm() {
			return siteTerm;
		}
		
	}

	public static final Comparator getUserSitesRowComparator(final String fieldName, final boolean sortAscending, final Collator collator) {
		return new Comparator() {
			public int compare(Object o1, Object o2) {
				if(o1 instanceof UserSitesRow && o2 instanceof UserSitesRow){
					UserSitesRow r1 = (UserSitesRow) o1;
					UserSitesRow r2 = (UserSitesRow) o2;
					try{
						if(fieldName.equals(SORT_SITE_NAME)){
							String s1 = r1.getSiteTitle();
							String s2 = r2.getSiteTitle();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_TYPE)){
							String s1 = r1.getSiteType();
							String s2 = r2.getSiteType();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_RID)){
							String s1 = r1.getRoleName();
							String s2 = r2.getRoleName();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_PV)){
							String s1 = r1.getPubView();
							String s2 = r2.getPubView();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_USER_STATUS)){
							String s1 = r1.getUserStatus();
							String s2 = r2.getUserStatus();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}else if(fieldName.equals(SORT_SITE_TERM)){
							String s1 = r1.getSiteTerm();
							String s2 = r2.getSiteTerm();
							int res = collator.compare(s1!=null? s1.toLowerCase():"", s2!=null? s2.toLowerCase():"");
							if(sortAscending) return res;
							else return -res;
						}
					}catch(Exception e){
						LOG.warn("Error occurred while sorting by: "+fieldName, e);
					}
				}
				return 0;
			}
		};
	}

	// ######################################################################################
	// Main methods
	// ######################################################################################
	
	public String getInitValues() {
		if(isAllowed()){
			if(userId == null){
				String param = (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("userId");
				if(param != null){
					userId = param;
				}
			}
	
			if(refreshQuery){
				LOG.debug("Refreshing query...");
				try{
					doSearch2();
				}catch(SQLException e){
					LOG.warn("Failed to perform search on usermembership", e);
				}
				refreshQuery = false;
			}
			
			if(userSitesRows != null && userSitesRows.size() > 0) Collections.sort(userSitesRows, getUserSitesRowComparator(sitesSortColumn, sitesSortAscending, collator));
		}
		return "";
	}
	
	

	/**
	 * Uses ONLY Sakai API for site membership, user role and group membership.
	 * @throws SQLException 
	 */
	private void doSearch2() throws SQLException {
		long start = (new Date()).getTime();
		userSitesRows = new ArrayList();
		thisUserId = M_session.getCurrentSessionUserId();
		setSakaiSessionUser(userId);
		LOG.debug("Switched CurrentSessionUserId: " + M_session.getCurrentSessionUserId());
		List siteList = org.sakaiproject.site.cover.SiteService.getSites(SelectionType.ACCESS_ALL, null, null, null, SortType.TITLE_ASC, null);
		setSakaiSessionUser(thisUserId);

		Iterator i = siteList.iterator();
		while (i.hasNext()){
			Site s = (Site) i.next();
			UserSitesRow row = new UserSitesRow(s, getGroups(userId, s), getActiveUserRoleInSite(userId, s));
			userSitesRows.add(row);
		}
		long end = (new Date()).getTime();
		LOG.debug("doSearch2() took total of "+((end - start)/1000)+" sec.");
	}

	

	/**
	 * Uses Sakai API for getting group membership
	 * @param userId The user ID.
	 * @param site The Site object
	 * @return A String with group list.
	 */
	public String getGroups(String userId, Site site) {
		long start = (new Date()).getTime();
		StringBuilder groups = new StringBuilder();
		Iterator ig = site.getGroupsWithMember(userId).iterator();
		while (ig.hasNext()){
			Group g = (Group) ig.next();
			if(groups.length() != 0) groups.append(", ");
			groups.append(g.getTitle());
			
			//NYU mod, get the sections_eid property for the group
			ResourceProperties props = g.getProperties();

			String sectionEid = (String) props.getProperty("sections_eid");
			if(StringUtils.isNotBlank(sectionEid)) {
					groups.append("("+sectionEid+")");
			}
		}
		long end = (new Date()).getTime();
		timeSpentInGroups += (end - start);
		LOG.debug("getGroups("+userId+", "+site.getTitle()+") took "+((end - start)/1000)+" sec.");
		return groups.toString();
	}
	
	

	/**
	 * Uses Sakai API for getting user role in site.
	 * @param userId The user ID.
	 * @param site The Site object.
	 * @return The user role in site as String.
	 */
	protected String getActiveUserRoleInSite(String userId, Site site) {
		Role r = site.getUserRole(userId);
		return (r != null) ? r.getId() : "";
	}

	private synchronized void setSakaiSessionUser(String id) {
		Session sakaiSession = M_session.getCurrentSession();
		sakaiSession.setUserId(id);
		sakaiSession.setUserEid(id);
	}

	// ######################################################################################
	// ActionListener methods
	// ######################################################################################
	public String processActionUserId() {
		try{
			ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
			Map paramMap = context.getRequestParameterMap();
			userId = (String) paramMap.get("userId");
			refreshQuery = true;
			return "sitelist";
		}catch(Exception e){
			LOG.error("Error getting userId var.");
			return "userlist";
		}
	}

	public String processActionBack() {
		return "userlist";
	}

	// ######################################################################################
	// Generic get/set methods
	// ######################################################################################
	public boolean isAllowed() {
		allowed = authz.isUserAbleToViewUmem(M_tm.getCurrentPlacement().getContext());
		
		if(!allowed){
			FacesContext fc = FacesContext.getCurrentInstance();
			message = msgs.getString("unauthorized");
			fc.addMessage("allowed", new FacesMessage(FacesMessage.SEVERITY_FATAL, message, null));
			allowed = false;
		}
		return allowed;
	}

	public List getUserSitesRows() {		
		if(userSitesRows != null && userSitesRows.size() > 0) Collections.sort(userSitesRows, getUserSitesRowComparator(sitesSortColumn, sitesSortAscending, collator));
		return userSitesRows;
	}

	public void setUserSitesRows(List userRows) {
		this.userSitesRows = userRows;
	}

	public boolean isEmptySiteList() {
		return (userSitesRows == null || userSitesRows.size() <= 0);
	}

	public boolean isRenderTable() {
		return !isEmptySiteList();
	}

	public String getUserDisplayId() {
		String displayId = null;
		try{
			displayId = UserDirectoryService.getUser(userId).getDisplayId();
		}catch(UserNotDefinedException e){
			displayId = userId;
		}
		return displayId;
	}

	public void setUserId(String id) {
		this.userId = id;
	}

	public boolean isSitesSortAscending() {
		return this.sitesSortAscending;
	}

	public void setSitesSortAscending(boolean sitesSortAscending) {
		this.sitesSortAscending = sitesSortAscending;
	}

	public String getSitesSortColumn() {
		return this.sitesSortColumn;
	}

	public void setSitesSortColumn(String sitesSortColumn) {
		this.sitesSortColumn = sitesSortColumn;
	}

	// ######################################################################################
	// CSV export
	// ######################################################################################
	public void exportAsCsv(ActionEvent event) {
		Export.writeAsCsv(buildDataTable(userSitesRows), getFileNamePrefix());
	}

	public void exportAsXls(ActionEvent event) {
		Export.writeAsXls(buildDataTable(userSitesRows), getFileNamePrefix());
	}
	
	private String getFileNamePrefix() {
		return "Membership_for_"+getUserDisplayId();
	}
	
	/**
	 * Build a generic tabular representation of the user site membership data export.
	 * 
	 * @param userSites The content of the table
	 * @return
	 * 	A table of data suitable to be exported
	 */
	private List<List<Object>> buildDataTable(List<UserSitesRow> userSites) {
		List<List<Object>> table = new LinkedList<List<Object>>();
		
		List<Object> header = new ArrayList<Object>();
		header.add(msgs.getString("site_name"));
		header.add(msgs.getString("site_id"));
		header.add(msgs.getString("groups"));
		header.add(msgs.getString("site_type"));
		header.add(msgs.getString("site_term"));
		header.add(msgs.getString("role_name"));
		header.add(msgs.getString("status"));
		header.add(msgs.getString("site_user_status"));
		table.add(header);
		
		for (UserSitesRow userSiteRow : userSites) {
			List<Object> currentRow = new ArrayList<Object>();
			currentRow.add(userSiteRow.getSiteTitle());
			currentRow.add(userSiteRow.getSiteId());
			currentRow.add(userSiteRow.getGroups());
			currentRow.add(userSiteRow.getSiteType());
			currentRow.add(userSiteRow.getSiteTerm());
			currentRow.add(userSiteRow.getRoleName());
			currentRow.add(userSiteRow.getPubView());
			currentRow.add(userSiteRow.getUserStatus());
			table.add(currentRow);
		}
		
		return table;
	}	
}
