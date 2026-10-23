package com.gathin.tvhome;

import android.service.notification.NotificationListenerService;

/** Explicit, user-enabled access. Notification content is never read, stored or sent anywhere. */
public final class NowPlayingAccess extends NotificationListenerService {
    static Runnable connectionChanged;
    @Override public void onListenerConnected() { if (connectionChanged!=null) connectionChanged.run(); }
    @Override public void onListenerDisconnected() { if (connectionChanged!=null) connectionChanged.run(); }
}
