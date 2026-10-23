package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

/** Opening a settings screen never writes route, volume, network or input settings. */
final class SystemLinks {
    static void open(Activity activity,Intent intent) {
        try { activity.startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException error) {
            fallback(activity,intent);
        }
    }
    private static void fallback(Activity activity,Intent original) {
        if (!Settings.ACTION_SETTINGS.equals(original.getAction())) {
            try { activity.startActivity(new Intent(Settings.ACTION_SETTINGS)); return; }
            catch (ActivityNotFoundException | SecurityException unavailable) { /* Show truthful unavailable state. */ }
        }
        Toast.makeText(activity,activity.getString(R.string.hc_unavailable),Toast.LENGTH_SHORT).show();
    }
    static void sound(Activity activity) {
        if (tclSound(activity)) return;
        // This TCL build publishes SoundActivity in its manifest, but the class is absent from its DEX.
        if (android.os.Build.MANUFACTURER.equalsIgnoreCase("TCL")) { open(activity,tclSettings(activity)); return; }
        open(activity,new Intent(Settings.ACTION_SOUND_SETTINGS));
    }
    private static boolean tclSound(Activity activity) {
        Intent panel=new Intent("com.tcl.settings.SHOW_WINDOW")
                .setClassName("com.tcl.settings","com.tcl.settings.ShowWindowService")
                .putExtra("Type","sound").putExtra("exitStartGoogleSettings",false);
        android.content.pm.ResolveInfo resolved=activity.getPackageManager().resolveService(panel,0);
        if (resolved==null || resolved.serviceInfo==null || !resolved.serviceInfo.exported) return false;
        try { return activity.startService(panel)!=null; }
        catch (SecurityException | IllegalStateException unavailable) { return false; }
    }
    private static Intent tclSettings(Activity activity) {
        Intent own=activity.getPackageManager().getLaunchIntentForPackage("com.tcl.settings");
        return own==null ? new Intent(Settings.ACTION_SETTINGS) : own;
    }
    static void wifi(Activity activity) { open(activity,new Intent(Settings.ACTION_WIFI_SETTINGS)); }
    static void tv(Activity activity) {
        Intent intent=SystemCapabilities.liveTv(activity);
        if (intent!=null) open(activity,intent);
        else open(activity,new Intent(Settings.ACTION_SETTINGS));
    }
}
