package org.sakaiproject.portal.charon.alerts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.db.api.SqlService;

public class AlertSystem {

  private SqlService sqlService;

  public AlertSystem() {
    if(sqlService == null) {
      sqlService = (SqlService) org.sakaiproject.component.cover.ComponentManager.get("org.sakaiproject.db.api.SqlService");
    }
  }
  
  public List<BannerAlert> getActiveBannerAlertsForServer(String serverId) {
    List<BannerAlert> alerts = new ArrayList<BannerAlert>();

    try {
      Connection db = sqlService.borrowConnection();

      try {
        PreparedStatement ps = db.prepareStatement("select * from NYU_T_BANNER_ALERT");

        ResultSet rs = ps.executeQuery();
        try {
          while (rs.next()) {
            BannerAlert alert = new BannerAlert(rs.getString("id"),
                                                rs.getString("message"),
                                                rs.getString("hosts"),
                                                rs.getInt("active"),
                                                rs.getInt("dismissible"));

            if (alert.isActiveForHost(serverId)) {
              alerts.add(alert); 
            }
          }
        } finally {
          rs.close();
        }
      } finally {
        sqlService.returnConnection(db);
      }
    } catch (SQLException e) {
      System.err.println(e);
    }

    return alerts;
  }
  
}
