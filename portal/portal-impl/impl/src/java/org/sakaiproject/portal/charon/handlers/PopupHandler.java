package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.nyu.classes.popup.api.PopupManager;
import edu.nyu.classes.popup.impl.DBPopupManager;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class PopupHandler extends BasePortalHandler
{
  private static final Log LOG = LogFactory.getLog(PopupHandler.class);

  private static final String URL_FRAGMENT = "popupAcknowledge";

  public PopupHandler()
  {
    setUrlFragment(PopupHandler.URL_FRAGMENT);
  }


  @Override
  public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
                   Session session) throws PortalHandlerException
  {
    return NEXT;
  }

  @Override
  public int doPost(String[] parts, HttpServletRequest req, HttpServletResponse res,
                   Session session) throws PortalHandlerException
  {

    if ((parts.length != 2) || (!URL_FRAGMENT.equals(parts[1]))) {
      return NEXT;
    }

    Object sessionToken = SessionManager.getCurrentSession().getAttribute("sakai.csrf.token");

    if (sessionToken == null || !((String)sessionToken).equals(req.getParameter("sakai_csrf_token"))) {
      LOG.warn("CSRF token validation failed");
      return END;
    }

    // headers
    res.setContentType("application/plain; charset=UTF-8");

    User currentUser = UserDirectoryService.getCurrentUser();

    PopupManager popups = new DBPopupManager(currentUser);

    popups.acknowledge(req.getParameter("campaign"), req.getParameter("acknowledgement"));

    return END;
  }
}
