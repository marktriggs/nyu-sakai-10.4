package edu.nyu.classes.groupersync;

import java.util.Date;

class UpdatedSite {
    private Date updateTime;
    private String siteId;

    public UpdatedSite(String siteId, Date updateTime) {
        this.siteId = siteId;
        this.updateTime = updateTime;
    }

    public String getSiteId() {
        return siteId;
    }
}
