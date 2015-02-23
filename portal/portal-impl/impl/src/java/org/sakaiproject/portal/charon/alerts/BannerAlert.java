package org.sakaiproject.portal.charon.alerts;

import java.util.Arrays;

public class BannerAlert {
  public String id; 
  public String message; 

  private String hosts; 
  private boolean isActive;

  public BannerAlert(String id, String message, String hosts, int active) {
    this.id = id;
    this.message = message;
    this.hosts = hosts;
    this.isActive = (active == 1);
  }
  
  public boolean isActive() {
    return this.isActive;
  }

  public boolean isActiveForHost(String hostname) {
    // are we active?
    if (!isActive()) {
      return false;
    }

    // if no hosts then assume active for any host
    if (this.hosts == null || this.hosts.isEmpty()) {
      return true;
    }

    // we have some hosts defined, so check if the current is listed
    return Arrays.asList(this.hosts.split(",")).contains(hostname);
  }
}
