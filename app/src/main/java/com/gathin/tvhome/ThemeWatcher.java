package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;

/** Keep every open native surface on the same palette in the preference change's UI turn. */
final class ThemeWatcher implements SharedPreferences.OnSharedPreferenceChangeListener, AutoCloseable {
    private final Context context;
    private final SharedPreferences preferences;
    private final Runnable changed;
    private String signature;
    ThemeWatcher(Context context,Runnable changed) {
        this.context=context; this.changed=changed;
        preferences=context.getSharedPreferences("living_room",Context.MODE_PRIVATE);
        signature=signature(); preferences.registerOnSharedPreferenceChangeListener(this);
    }
    private String signature() { return preferences.getString("theme","dark")+":"+TvPalette.of(context).light+":"+new TvPreferences(context).widgetSignature(); }
    void refresh() {
        String next=signature();
        if (next.equals(signature)) return;
        signature=next; changed.run();
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences values,String key) {
        if (key==null || key.equals("theme") || key.startsWith("widget_")) refresh();
    }
    @Override public void close() { preferences.unregisterOnSharedPreferenceChangeListener(this); }
}
