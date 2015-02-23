package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.portal.charon.alerts.AlertSystem;
import org.sakaiproject.portal.charon.alerts.BannerAlert;
import org.sakaiproject.tool.api.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.sakaiproject.component.cover.ServerConfigurationService;

import java.io.IOException;
import java.io.PrintWriter;

public class SystemAlertHandler extends BasePortalHandler
{
  private static final Log log = LogFactory.getLog(SystemAlertHandler.class);
  private static final String URL_FRAGMENT = "system-alerts";

  public SystemAlertHandler()
  {
    setUrlFragment(URL_FRAGMENT);
  }

  public int doPost(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session)
  throws PortalHandlerException
  {
    return NEXT;
  }

  public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session)
  throws PortalHandlerException
  {
    if ((parts.length == 3) && (URL_FRAGMENT.equals(parts[1])))
    {
      if ("banner".equals(parts[2])) {
        AlertSystem alertSystem = new AlertSystem();
        JSONArray alerts = new JSONArray();
        String serverId = ServerConfigurationService.getString("serverId","localhost");

        for (BannerAlert alert : alertSystem.getActiveBannerAlertsForServer(serverId)) {
          JSONObject alertData = new JSONObject();
          alertData.put("id", alert.id);
          alertData.put("message", alert.message);
          alerts.add(alertData);
        }

        // json response
        res.setContentType("application/json; charset=UTF-8");
        // cache for 10 minutes
        //res.addDateHeader("Expires", System.currentTimeMillis() + (1000L * 60L * 10L));
        //res.addHeader("Cache-Control", "max-age=" + (60L * 10L) + ", must-revalidate");
        //.. ok.. let's not cache for now
        res.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        res.addHeader("Pragma", "no-cache");
        res.addDateHeader("Expires", 0);

        try {
          PrintWriter pw = res.getWriter();
          pw.write(alerts.toJSONString());
          pw.close();
        } catch(IOException e) {
          throw new PortalHandlerException();
        }

        return END;
      }
    }

    return NEXT;
  }
}