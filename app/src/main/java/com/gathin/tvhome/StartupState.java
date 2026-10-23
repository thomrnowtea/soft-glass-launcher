package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/** Opt-in startup and evidence of an actual MainActivity resume, separate from appearance settings. */
final class StartupState {
    static final String TAG = "LauncherStartup";
    static final String REQUEST = BuildConfig.APPLICATION_ID+".STARTUP_REQUEST";

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("startup", Context.MODE_PRIVATE);
    }

    static int bootCount(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }

    static boolean enabled(Context context) {
        return preferences(context).getBoolean("enabled", false);
    }

    /** Prime the current boot so configuration/update never brings Home over the user's current app. */
    static boolean configure(Context context, boolean enabled) {
        int boot = bootCount(context);
        if (enabled && boot < 0) return false;
        return preferences(context).edit().putBoolean("enabled", enabled)
                .putInt("handled_boot", boot).commit();
    }

    /** At most one initial attempt per Android boot, including service reconnections and APK updates. */
    static boolean claimNewBoot(Context context) {
        int boot = bootCount(context);
        SharedPreferences values = preferences(context);
        if (!enabled(context) || boot < 0 || values.getInt("handled_boot", -1) == boot) return false;
        return values.edit().putInt("handled_boot", boot).commit();
    }

    static long beginAttempt(Context context, String reason) {
        long token = SystemClock.elapsedRealtime();
        preferences(context).edit().putLong("attempt_uptime", token)
                .putInt("attempt_boot", bootCount(context)).putString("attempt_reason", reason).apply();
        Log.i(TAG, "Start requested: " + reason + ", boot=" + bootCount(context));
        return token;
    }

    static void recordResume(Context context, Intent intent) {
        if (intent == null || !intent.hasExtra(REQUEST)) return;
        long token = intent.getLongExtra(REQUEST, -1);
        intent.removeExtra(REQUEST);
        SharedPreferences values = preferences(context);
        long age = SystemClock.elapsedRealtime() - token;
        if (token < 0 || token != values.getLong("attempt_uptime", -2)
                || age < 0 || age > 12_000 || values.getInt("attempt_boot", -1) != bootCount(context)) return;
        values.edit().putLong("resume_uptime", SystemClock.elapsedRealtime())
                .putInt("resume_boot", bootCount(context)).apply();
        Log.i(TAG, "Home resumed after startup request, boot=" + bootCount(context));
    }
}
