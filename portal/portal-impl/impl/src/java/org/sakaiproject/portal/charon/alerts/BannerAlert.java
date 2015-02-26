package org.sakaiproject.portal.charon.alerts;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;

public class BannerAlert {
  public String id; 
  public String message; 

  private String hosts; 
  private boolean isActive;
  private boolean isDismissible;
  private Timestamp activeFrom;
  private Timestamp activeUntil;

  public BannerAlert(String id, String message, String hosts, int dismissible, int active, Timestamp activeFrom, Timestamp activeUntil) {
    this.id = id;
    this.message = message;
    this.hosts = hosts;
    this.isActive = (active == 1);
    this.isDismissible = (dismissible == 1);
    this.activeFrom = activeFrom;
    this.activeUntil = activeUntil;
  }

  public boolean isActive() {
    if (activeFrom == null && activeUntil == null) {
      return isActive;
    }

    Timestamp now = new Timestamp(new Date().getTime());

    return (activeFrom == null || now.after(activeFrom))
            && (activeUntil == null || now.before(activeUntil));
  }

  public boolean isDismissible() {
    return this.isDismissible;
  }

  public boolean isActiveForHost(String hostname) {
    // are we active?
    if (!isActive()) {
      return false;
    }

    // if no hosts then assume active for any host
    if (hosts == null || hosts.isEmpty()) {
      return true;
    }

    // we have some hosts defined, so check if the current is listed
    return Arrays.asList(hosts.split(",")).contains(hostname);
  }
}
