package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

/** Android gates the SSID behind location permission; this code never requests a location fix. */
final class NetworkNameAccess {
    static final int REQUEST=4208;
    static boolean granted(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    static boolean locationEnabled(Context context) {
        LocationManager manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        if (manager==null) return false;
        if (Build.VERSION.SDK_INT>=28) return manager.isLocationEnabled();
        return Settings.Secure.getInt(context.getContentResolver(),Settings.Secure.LOCATION_MODE,0)!=0;
    }
    static void requestOnHome(Activity activity) {
        android.content.SharedPreferences prefs=activity.getSharedPreferences("living_room",0);
        if (granted(activity) || prefs.getBoolean("wifi_name_permission_requested",false)) return;
        prefs.edit().putBoolean("wifi_name_permission_requested",true).apply();
        request(activity);
    }
    static void request(Activity activity) {
        if (!granted(activity)) {
            activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION},REQUEST); return;
        }
        if (!locationEnabled(activity)) { SystemLinks.open(activity,new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); return; }
        android.widget.Toast.makeText(activity,TvUi.label(activity,"Wi-Fi name access enabled","Acceso al nombre Wi-Fi habilitado"),android.widget.Toast.LENGTH_SHORT).show();
    }
}
