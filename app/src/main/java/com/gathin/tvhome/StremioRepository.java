package com.gathin.tvhome;

import android.content.Context;
import java.io.IOException;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Reads the owner's library. No datastore writes, watched-state mutations or logout calls. */
final class StremioRepository {
    private final StremioStore store;
    StremioRepository(Context context) { store = new StremioStore(context); }
    List<ResumeItem> load() throws IOException {
        String account = store.key();
        if (account.isEmpty()) return store.cached();
        long age = System.currentTimeMillis() - store.saved();
        if (age >= 0 && age < 60000) return store.cached();
        try {
            JSONObject request = new JSONObject().put("authKey", account).put("collection", "libraryItem")
                    .put("ids", new JSONArray()).put("all", true);
            JSONObject response = NetJson.post("https://api.strem.io/api/datastoreGet", request);
            JSONArray data = response.optJSONArray("result");
            if (data == null) throw new IOException("Stremio could not synchronize");
            List<ResumeItem> items = ResumeItem.decode(data);
            if (!store.current(account)) throw new IOException("Account changed");
            store.save(account, items); return items;
        } catch (JSONException error) { throw new IOException("Invalid Stremio library"); }
    }
}
