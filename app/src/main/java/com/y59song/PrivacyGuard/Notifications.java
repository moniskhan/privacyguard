package com.y59song.PrivacyGuard;

/**
 * Created by MAK on 20/10/2015.
 */
public interface Notifications {

    void notify(String appName, String msg);

    void updateNotification(String appName, String msg);

    void deleteNotification(int id);
}
