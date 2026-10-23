package com.gathin.tvhome;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

/** Check the actual access grant and offer only settings destinations published by this firmware. */
final class SpotifyAccess {
    private SpotifyAccess() { }
    static boolean granted(Context context) {
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        ComponentName component=new ComponentName(context,NowPlayingAccess.class);
        if (android.os.Build.VERSION.SDK_INT>=27) return manager!=null && manager.isNotificationListenerAccessGranted(component);
        String enabled=Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
        if (enabled==null) return false;
        for (String item:enabled.split(":")) if (component.equals(ComponentName.unflattenFromString(item))) return true;
        return false;
    }
    static String summary(Context context) {
        boolean enabled=granted(context);
        return TvUi.label(context,enabled ? "Playback access enabled" : "Playback access is off",
                enabled ? "Acceso a reproducción habilitado" : "Acceso a reproducción desactivado");
    }
    static Intent settings(Context context) {
        Intent detail=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,new ComponentName(context,NowPlayingAccess.class).flattenToString());
        if (available(context,detail)) return detail;
        Intent list=new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        return available(context,list) ? list : null;
    }
    private static boolean available(Context context,Intent intent) {
        ResolveInfo info=context.getPackageManager().resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);
        return info!=null && info.activityInfo!=null && info.activityInfo.exported;
    }
}
