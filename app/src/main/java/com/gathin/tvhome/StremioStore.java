package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** App-private account and read-only resume cache; excluded from Android backup. */
final class StremioStore {
    private static final Object LOCK = new Object();
    private final SharedPreferences prefs;
    StremioStore(Context context) { prefs = context.getSharedPreferences("stremio_resume", Context.MODE_PRIVATE); }
    String key() { synchronized (LOCK) { return prefs.getString("key", ""); } }
    boolean linked() { return !key().isEmpty(); }
    void link(String key) {
        synchronized (LOCK) { prefs.edit().clear().putString("key", key).commit(); }
    }
    void unlink() { synchronized (LOCK) { prefs.edit().clear().commit(); } }
    boolean current(String key) { return !key.isEmpty() && key.equals(key()); }
    long saved() { return prefs.getLong("saved", 0); }
    List<ResumeItem> cached() {
        synchronized (LOCK) {
            if (!linked()) return new ArrayList<>();
            try { return ResumeItem.decode(new JSONArray(prefs.getString("items", "[]"))); }
            catch (JSONException error) { return new ArrayList<>(); }
        }
    }
    void save(String account, List<ResumeItem> items) {
        synchronized (LOCK) {
            if (!current(account)) return;
            JSONArray result = new JSONArray();
            for (ResumeItem item : items) result.put(item.json);
            prefs.edit().putString("items", result.toString()).putLong("saved", System.currentTimeMillis()).apply();
        }
    }
}
