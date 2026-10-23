package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.media.tv.TvContract;
import android.provider.Settings;
import java.util.HashSet;
import java.util.Set;

/** Read-only platform discovery. Never changes the default Home or vendor settings. */
final class SystemCapabilities {
    static boolean resolves(Context context,Intent intent) {
        ResolveInfo found=context.getPackageManager().resolveActivity(intent,0);
        return found!=null && found.activityInfo!=null && found.activityInfo.exported;
    }
    static boolean installed(Context context,String pkg) {
        return context.getPackageManager().getLeanbackLaunchIntentForPackage(pkg)!=null
                || context.getPackageManager().getLaunchIntentForPackage(pkg)!=null;
    }
    static boolean homeSettings(Context context) { return resolves(context,new Intent(Settings.ACTION_HOME_SETTINGS)); }
    static Intent liveTv(Context context) {
        Intent generic=new Intent(Intent.ACTION_VIEW,TvContract.Channels.CONTENT_URI);
        if (resolves(context,generic)) return generic;
        String configured=Config.load(context).liveTvPackage;
        if (!configured.isEmpty()) {
            Intent entry=context.getPackageManager().getLeanbackLaunchIntentForPackage(configured);
            if (entry!=null) return entry;
        }
        if (android.os.Build.MANUFACTURER.equalsIgnoreCase("TCL")) {
            return context.getPackageManager().getLaunchIntentForPackage("com.tcl.tv");
        }
        return null;
    }
    static Set<String> stockHomes(Context context) {
        Set<String> result=new HashSet<>();
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo entry:context.getPackageManager().queryIntentActivities(home,0)) {
            if (entry.activityInfo==null) continue;
            String pkg=entry.activityInfo.packageName;
            if (pkg.equals(context.getPackageName())) continue;
            if ((entry.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)!=0) result.add(pkg);
        }
        return result;
    }
    static boolean defaultHome(Context context) {
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolved=context.getPackageManager().resolveActivity(home,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return resolved!=null && resolved.activityInfo!=null && context.getPackageName().equals(resolved.activityInfo.packageName);
    }
}
