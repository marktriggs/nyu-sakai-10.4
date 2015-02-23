package org.sakaiproject.portal.charon.handlers;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;

import org.sakaiproject.user.api.User;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.site.api.Site;

import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.user.cover.UserDirectoryService;

import java.util.TimeZone;
import java.util.Date;

import java.io.IOException;

public class TimezoneCheckHandler extends BasePortalHandler
{

  private static final String URL_FRAGMENT = "timezoneCheck";

  public TimezoneCheckHandler()
  {
    setUrlFragment(TimezoneCheckHandler.URL_FRAGMENT);
  }


  private String getTimezoneToolUrlForUser()
  {
    User thisUser = UserDirectoryService.getCurrentUser();
    String userid = thisUser.getId();
    try {
      Site userSite = SiteService.getSite("~"+userid);
      ToolConfiguration preferences = userSite.getToolForCommonId("sakai.preferences");
      return String.format("/portal/tool/%s/timezone", preferences.getId());
    } catch (Exception ex) {
      System.err.println("** EEEEP. Exception trying to get sakai.preferences tool for: " + userid);
      ex.printStackTrace();
    }
    
    return "";
  }


  private String formatOffset(TimeZone tz)
  {
    long now = new Date().getTime();

    long offset = tz.getOffset(now);

    int mins = 60 * 1000;
    int hour = 60 * mins;
    return "(GMT " + String.format("%s%0,2d:%0,2d",
                                  ((offset >= 0) ? "+" : ""),
                                  (offset / hour),
                                  ((offset % hour) / mins)) + ")";
  }


  @Override
  public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
                   Session session) throws PortalHandlerException
  {

    if ((parts.length != 2) || (!URL_FRAGMENT.equals(parts[1]))) {
      return NEXT;
    }

    // headers
    res.setContentType("application/json; charset=UTF-8");
    res.addDateHeader("Expires", System.currentTimeMillis()
                      - (1000L * 60L * 60L * 24L * 365L));
    res.addDateHeader("Last-Modified", System.currentTimeMillis());
    res.addHeader("Cache-Control",
                  "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
    res.addHeader("Pragma", "no-cache");

    try {
      String timezoneFromUser = req.getParameter("timezone");

      if (timezoneFromUser == null) {
        return END;
      }

      PrintWriter out = res.getWriter();
      TimeZone preferredTimeZone = TimeService.getLocalTimeZone();
      TimeZone reportedTimeZone = TimeZone.getTimeZone(timezoneFromUser);

      String setTimezoneUrl = getTimezoneToolUrlForUser();

      long now = new Date().getTime();

      if (preferredTimeZone.getOffset(now) == reportedTimeZone.getOffset(now)) {
        out.println("{\"status\": \"OK\"}");
      } else {
        out.println(String.format("{" +
                                  "\"status\": \"MISMATCH\", " +
                                  "\"setTimezoneUrl\": \"%s\", " +
                                  "\"prefs_timezone\": \"%s\"," +
                                  "\"reported_timezone\": \"%s\"" +
                                  "}",
                                  setTimezoneUrl,
                                  preferredTimeZone.getID() + " " + formatOffset(preferredTimeZone),
                                  formatOffset(reportedTimeZone)));
      }
    } catch (IOException ex) {
      throw new PortalHandlerException(ex);
    }

    return END;
  }
}
