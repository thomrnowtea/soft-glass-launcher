package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/** Launch once at boot; on wake, a bounded opportunity only when the stock Home appears. */
public final class StartupHomeService extends AccessibilityService {
    private java.util.Set<String> stockHomes=java.util.Collections.emptySet();
    private static final long WAKE_WINDOW_MS = 10_000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable expire = this::cancelOpportunity;
    private String opportunity;
    private long deadline;
    private boolean receiverRegistered;
    private SharedPreferences preferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener configuration = (values, key) -> {
        if ("enabled".equals(key)) updateRegistration();
    };
    private final BroadcastReceiver screen = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                cancelOpportunity();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction()) && StartupState.enabled(context)) {
                if ("boot".equals(opportunity)) launch("boot");
                else arm("wake");
            }
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        stockHomes=SystemCapabilities.stockHomes(this);
        android.accessibilityservice.AccessibilityServiceInfo info=getServiceInfo();
        info.packageNames=stockHomes.isEmpty() ? new String[]{getPackageName()} : stockHomes.toArray(new String[0]);
        setServiceInfo(info);
        preferences = StartupState.preferences(this);
        preferences.registerOnSharedPreferenceChangeListener(configuration);
        updateRegistration();
        if (StartupState.claimNewBoot(this)) {
            if (interactive()) launch("boot");
            else arm("boot");
        }
        Log.i(StartupState.TAG, "Service connected, enabled=" + StartupState.enabled(this));
    }

    private void updateRegistration() {
        cancelOpportunity();
        if (!StartupState.enabled(this)) {
            unregisterScreen();
            return;
        }
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screen, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screen, filter);
        receiverRegistered = true;
    }

    private boolean interactive() {
        PowerManager power = getSystemService(PowerManager.class);
        return power != null && power.isInteractive();
    }

    private void arm(String reason) {
        cancelOpportunity();
        opportunity = reason;
        deadline = SystemClock.elapsedRealtime() + WAKE_WINDOW_MS;
        handler.postDelayed(expire, WAKE_WINDOW_MS);
    }

    private void cancelOpportunity() {
        handler.removeCallbacks(expire);
        opportunity = null;
        deadline = 0;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (opportunity == null || SystemClock.elapsedRealtime() > deadline) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || event.getPackageName()==null || !stockHomes.contains(event.getPackageName().toString())) return;
        if (StartupState.enabled(this) && interactive()) launch(opportunity);
    }

    private void launch(String reason) {
        cancelOpportunity();
        Intent home = new Intent(this, MainActivity.class).setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .putExtra(StartupState.REQUEST, StartupState.beginAttempt(this, reason));
        try {
            startActivity(home);
        } catch (ActivityNotFoundException | SecurityException error) {
            Log.e(StartupState.TAG, "Startup launch rejected; no retry loop", error);
        }
    }

    private void unregisterScreen() {
        if (!receiverRegistered) return;
        unregisterReceiver(screen);
        receiverRegistered = false;
    }

    @Override public void onInterrupt() { cancelOpportunity(); }

    @Override public void onDestroy() {
        cancelOpportunity();
        unregisterScreen();
        if (preferences != null) preferences.unregisterOnSharedPreferenceChangeListener(configuration);
        super.onDestroy();
    }

    @Override protected void dump(FileDescriptor descriptor, PrintWriter output, String[] args) {
        output.println("Startup enabled=" + StartupState.enabled(this));
        output.println("Boot=" + StartupState.bootCount(this) + " handled="
                + StartupState.preferences(this).getInt("handled_boot", -1));
        output.println("Screen receiver=" + receiverRegistered + " opportunity=" + opportunity);
        output.println("Last attempt boot=" + StartupState.preferences(this).getInt("attempt_boot", -1));
        output.println("Last resumed boot=" + StartupState.preferences(this).getInt("resume_boot", -1));
    }
}
