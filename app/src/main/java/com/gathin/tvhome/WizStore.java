package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Only identities, local addresses, capabilities and user aliases; no WiZ cloud credentials. */
final class WizStore {
    private final SharedPreferences preferences;
    WizStore(Context context) {
        preferences = context.getSharedPreferences("wiz_local", Context.MODE_PRIVATE);
    }

    List<WizBulb> load() {
        List<WizBulb> bulbs = new ArrayList<>();
        try {
            JSONArray saved = new JSONArray(preferences.getString("bulbs", "[]"));
            for (int index = 0; index < Math.min(saved.length(), 32); index++) {
                JSONObject item = saved.getJSONObject(index);
                bulbs.add(new WizBulb(item.getString("host"), item));
            }
        } catch (JSONException ignored) { /* Corrupt cache: rediscover without touching lights. */ }
        return bulbs;
    }

    void save(List<WizBulb> bulbs) {
        JSONArray saved = new JSONArray();
        try {
            for (WizBulb bulb : bulbs) saved.put(bulb.stored());
            preferences.edit().putString("bulbs", saved.toString()).apply();
        } catch (JSONException error) {
            throw new IllegalStateException("Invalid local light metadata", error);
        }
    }
}
