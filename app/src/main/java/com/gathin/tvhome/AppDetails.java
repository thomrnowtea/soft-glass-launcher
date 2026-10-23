package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** Open the app's native Android management screen. */
final class AppDetails {
    static void open(Activity activity,String pkg) {
        SystemLinks.open(activity,new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg)));
    }
    private AppDetails() { }
}
