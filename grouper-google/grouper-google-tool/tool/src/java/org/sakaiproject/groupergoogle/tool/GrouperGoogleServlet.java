package org.sakaiproject.groupergoogle.tool;

import java.io.IOException;
import java.lang.CharSequence;
import java.lang.Override;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Helper;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.site.api.Group;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.exception.IdUnusedException;

public class GrouperGoogleServlet extends HttpServlet {

	private static final Logger LOG = LoggerFactory.getLogger(GrouperGoogleServlet.class);

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		checkAccessControl();

		response.setHeader("Content-Type", "text/html");

		//URL toolBaseURL = determineBaseURL();
		Handlebars handlebars = loadHandlebars();

		String siteId = ToolManager.getCurrentPlacement().getContext();

		try {
			Site site = SiteService.getSite(siteId);
			Collection<Group> groups = site.getGroups();

			Map<String, Object> context = new HashMap<String, Object>();
			context.put("groups", groups);
			context.put("subpage", "index");

			context.put("skinRepo", ServerConfigurationService.getString("skin.repo", ""));
			context.put("randomSakaiHeadStuff", request.getAttribute("sakai.html.head"));

			Template template = handlebars.compile("org/sakaiproject/groupergoogle/tool/views/layout");
			response.getWriter().write(template.apply(context));
		} catch (IdUnusedException e) {
			LOG.warn("Site not found:" + siteId, e);
			throw new ServletException("Site not found:" + siteId);
		} catch (IOException e) {
			LOG.warn("Write failed", e);
		}
	}

	private void checkAccessControl() {
		//String siteId = ToolManager.getCurrentPlacement().getContext();
	}


	private Handlebars loadHandlebars() {
		final Handlebars handlebars = new Handlebars();

		handlebars.registerHelper("subpage", new Helper<Object>() {
			@Override
			public CharSequence apply(final Object context, final Options options) {
				String subpage = options.param(0);
				try {
					Template template = handlebars.compile("org/sakaiproject/groupergoogle/tool/views/" + subpage);
					return template.apply(context);
				} catch (IOException e) {
					LOG.warn("IOException while loading subpage", e);
					return "";
				}
			}
		});

		return handlebars;
	}
}