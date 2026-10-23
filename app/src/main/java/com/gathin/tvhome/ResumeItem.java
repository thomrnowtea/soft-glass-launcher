package com.gathin.tvhome;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Same resume predicate and millisecond progress as the official Stremio core. */
final class ResumeItem {
    final JSONObject json;
    final String id, type, name, video, modified;
    final long offset, duration;
    ResumeItem(JSONObject json) throws JSONException {
        this.json = json;
        id = json.getString("_id"); type = json.getString("type"); name = json.getString("name");
        JSONObject state = json.getJSONObject("state");
        offset = Math.max(0, state.optLong("timeOffset")); duration = Math.max(0, state.optLong("duration"));
        video = state.optString("video_id", "");
        modified = state.optString("lastWatched", json.optString("_mtime", ""));
        if (id.isEmpty() || id.length() > 256 || !type.matches("[A-Za-z0-9_-]{1,32}") || name.isEmpty())
            throw new JSONException("Invalid resume item");
    }
    boolean eligible() {
        return !type.equals("other") && (!json.optBoolean("removed") || json.optBoolean("temp")) && offset > 0;
    }
    int progress() { return duration > 0 ? (int)Math.min(100, 100d * offset / duration) : 0; }
    long remainingMinutes() { return duration > offset ? Math.max(1, (duration - offset + 59999) / 60000) : 0; }
    Uri detail() {
        String path = "stremio:///detail/" + Uri.encode(type) + "/" + Uri.encode(id);
        if (!video.isEmpty() && !video.equals("null")) path += "/" + Uri.encode(video);
        return Uri.parse(path);
    }
    static List<ResumeItem> decode(JSONArray data) {
        List<ResumeItem> result = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            try { ResumeItem item = new ResumeItem(data.getJSONObject(i)); if (item.eligible()) result.add(item); }
            catch (JSONException ignored) { }
        }
        result.sort(Comparator.comparing((ResumeItem item) -> item.modified).reversed());
        return new ArrayList<>(result.subList(0, Math.min(12, result.size())));
    }
}
