package edu.nyu.classes.popup.api;

import org.sakaiproject.user.api.User;

public interface PopupManager
{
    public Popup getPopup();

    public void acknowledge(String campaign, String acknowledgement);
}
