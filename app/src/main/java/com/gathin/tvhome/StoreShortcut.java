package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

final class StoreShortcut {
    static void open(Activity activity) {
        Intent intent=activity.getPackageManager().getLeanbackLaunchIntentForPackage("com.android.vending");
        if (intent==null) intent=activity.getPackageManager().getLaunchIntentForPackage("com.android.vending");
        if (intent==null) intent=new Intent(Intent.ACTION_VIEW,Uri.parse("market://search?q=apps")).setPackage("com.android.vending");
        try { activity.startActivity(intent); }
        catch (ActivityNotFoundException error) {
            android.widget.Toast.makeText(activity,TvUi.label(activity,"Play Store is not available","Play Store no está disponible"),android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
