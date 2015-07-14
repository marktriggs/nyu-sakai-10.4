package org.sakaiproject.lessonbuildertool.tool.producers;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.assignment.cover.AssignmentService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.lessonbuildertool.SimplePage;
import org.sakaiproject.lessonbuildertool.SimplePageItem;
import org.sakaiproject.lessonbuildertool.model.SimplePageToolDao;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean;
import org.sakaiproject.lessonbuildertool.tool.beans.SimplePageBean.Status;
import org.sakaiproject.lessonbuildertool.tool.view.FilePickerViewParameters;
import org.sakaiproject.lessonbuildertool.tool.view.GeneralViewParameters;
import org.sakaiproject.lessonbuildertool.tool.producers.PermissionsHelperProducer;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.lessonbuildertool.service.LessonEntity;

import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.portal.util.CSSUtils;
import org.sakaiproject.util.Web;

import uk.org.ponder.messageutil.MessageLocator;
import uk.org.ponder.localeutil.LocaleGetter;                                                                                          
import uk.org.ponder.rsf.components.UIBoundBoolean;
import uk.org.ponder.rsf.components.UIBranchContainer;
import uk.org.ponder.rsf.components.UIBoundString;
import uk.org.ponder.rsf.components.UICommand;
import uk.org.ponder.rsf.components.UIComponent;
import uk.org.ponder.rsf.components.UIContainer;
import uk.org.ponder.rsf.components.UIForm;
import uk.org.ponder.rsf.components.UIInput;
import uk.org.ponder.rsf.components.UIInternalLink;
import uk.org.ponder.rsf.components.UILink;
import uk.org.ponder.rsf.components.UIOutput;
import uk.org.ponder.rsf.components.UISelect;
import uk.org.ponder.rsf.components.UIVerbatim;
import uk.org.ponder.rsf.components.decorators.UIDisabledDecorator;
import uk.org.ponder.rsf.components.decorators.UIFreeAttributeDecorator;
import uk.org.ponder.rsf.components.decorators.UIStyleDecorator;
import uk.org.ponder.rsf.components.decorators.UITooltipDecorator;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCase;
import uk.org.ponder.rsf.flow.jsfnav.NavigationCaseReporter;
import uk.org.ponder.rsf.view.ComponentChecker;
import uk.org.ponder.rsf.view.DefaultView;
import uk.org.ponder.rsf.view.ViewComponentProducer;
import uk.org.ponder.rsf.viewstate.SimpleViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParameters;
import uk.org.ponder.rsf.viewstate.ViewParamsReporter;
import org.springframework.core.io.Resource;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import edu.nyu.classes.externalhelp.api.ExternalHelpSystem;
import edu.nyu.classes.externalhelp.api.ExternalHelp;

import org.sakaiproject.scormcloudservice.api.ScormCloudService;
import org.sakaiproject.scormcloudservice.api.ScormRegistrationNotFoundException;
import org.sakaiproject.scormcloudservice.api.ScormException;


public class ShowScormProducer implements ViewComponentProducer, NavigationCaseReporter, ViewParamsReporter {

	private static Log log = LogFactory.getLog(ShowScormProducer.class);

	private SimplePageBean simplePageBean;
	private SimplePageToolDao simplePageToolDao;
	public MessageLocator messageLocator;
	public LocaleGetter localeGetter;

	private HttpServletRequest httpServletRequest;
	private HttpServletResponse httpServletResponse;

	public static final String VIEW_ID = "ShowScorm";

	public String getViewID() {
		return VIEW_ID;
	}
    
	public void fillComponents(UIContainer tofill, ViewParameters viewParams, ComponentChecker checker) {
		GeneralViewParameters params = (GeneralViewParameters)viewParams;

		try {
			// Try to generate a "Return" button.  Apparently that's very hard.
			SimplePageBean.PathEntry backpath = ((List<SimplePageBean.PathEntry>)SessionManager.getCurrentToolSession().getAttribute(SimplePageBean.LESSONBUILDER_BACKPATH)).get(0);

			GeneralViewParameters view = new GeneralViewParameters(ShowPageProducer.VIEW_ID);
			view.setSendingPage(backpath.pageId);
			view.setItemId(backpath.pageItemId);

			UIInternalLink.make(tofill, "return", messageLocator.getMessage("simplepage.return"), view);		
		} catch (IndexOutOfBoundsException e) {
		}


		UIOutput.make(tofill, "scorm-title", simplePageBean.getName());	

		if (simplePageBean.canEditPage()) {
			markCourseForGradeSync(params.getItemId());
			showStatusPage(tofill, viewParams);
		} else {
			if ("true".equals(httpServletRequest.getParameter("returned"))) {
				UIOutput.make(tofill, "html").decorate(new UIFreeAttributeDecorator("lang", localeGetter.get().getLanguage()))
					.decorate(new UIFreeAttributeDecorator("xml:lang", localeGetter.get().getLanguage()));

				UIOutput.make(tofill, "scorm-item-completed");
				markCourseForGradeSync(params.getItemId());
			} else {
				redirectToPlayer(tofill, viewParams);
			}
		}
	}


	private void markCourseForGradeSync(Long currentItemId) {
		// Mark this course as 'touched' so grades will be propagated from SCORM cloud.
		//
		// There's no harm in doing this more than once (it just sets an
		// mtime that the Quartz job notices), so we do it in a few
		// cases just to make sure that active courses are resynced when
		// they might have changed.
		String currentSiteId = ToolManager.getCurrentPlacement().getContext();
		try {
			scormService().markCourseForGradeSync(currentSiteId, currentItemId.toString());
		} catch (ScormException e) {
			log.info("Failure when marking course for gradesync", e);
		}
	}

	private void showStatusPage(UIContainer tofill, ViewParameters viewParams) {
                UIOutput.make(tofill, "html").decorate(new UIFreeAttributeDecorator("lang", localeGetter.get().getLanguage()))
			.decorate(new UIFreeAttributeDecorator("xml:lang", localeGetter.get().getLanguage()));

		UIOutput.make(tofill, "scorm-item-status", messageLocator.getMessage("simplepage.scorm.new_status"));
	}

	private ScormCloudService scormService() {
		return (ScormCloudService)ComponentManager.get("org.sakaiproject.scormcloudservice.api.ScormCloudService");
	}

	private void redirectToPlayer(UIContainer tofill, ViewParameters viewParams) {
		try {
			String backlink = ServerConfigurationService.getServerUrl() + httpServletRequest.getRequestURI() + "?returned=true&" + httpServletRequest.getQueryString();

			GeneralViewParameters params = (GeneralViewParameters)viewParams;
			String currentSiteId = ToolManager.getCurrentPlacement().getContext();

			httpServletResponse.sendRedirect(scormService().getScormPlayerUrl(currentSiteId, params.getItemId().toString(), backlink));
		} catch (IOException e) {
		} catch (ScormRegistrationNotFoundException e) {
		} catch (ScormException e) {
		}
	}

	public void setSimplePageBean(SimplePageBean simplePageBean) {
		this.simplePageBean = simplePageBean;
	}

	public void setSimplePageToolDao(SimplePageToolDao s) {
		simplePageToolDao = s;
	}

	public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	public void setHttpServletResponse(HttpServletResponse httpServletResponse) {
		this.httpServletResponse = httpServletResponse;
	}

	public List reportNavigationCases() {
		List<NavigationCase> togo = new ArrayList<NavigationCase>();
		togo.add(new NavigationCase("success", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		togo.add(new NavigationCase("failure", new SimpleViewParameters(ShowItemProducer.VIEW_ID)));
		togo.add(new NavigationCase("cancel", new SimpleViewParameters(ShowPageProducer.VIEW_ID)));
		return togo;
	}

	public ViewParameters getViewParameters() {
		return new GeneralViewParameters();
	}
}
