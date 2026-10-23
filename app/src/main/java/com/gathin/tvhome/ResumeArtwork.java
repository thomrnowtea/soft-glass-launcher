package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Artwork lookup only for an actual resume item; never fetches recommendations. */
final class ResumeArtwork {
    private final Context context;
    private final SharedPreferences cache;
    ResumeArtwork(Context context) {
        this.context = context.getApplicationContext();
        cache = context.getSharedPreferences("resume_artwork",Context.MODE_PRIVATE);
    }
    String cached(ResumeItem item) { return cache.getString(item.type + ":" + item.id,""); }
    String load(ResumeItem item) throws IOException {
        String previous = cached(item);
        if (!previous.isEmpty()) return previous;
        if (!item.id.matches("tt[0-9]+")) return "";
        String art = tmdb(item);
        if (art.isEmpty()) art = cinemeta(item);
        if (!art.isEmpty()) cache.edit().putString(item.type + ":" + item.id,art).apply();
        return art;
    }
    private String tmdb(ResumeItem item) {
        String credential = new TmdbAuth(context).read();
        if (credential.isEmpty()) return "";
        try {
            boolean jwt = credential.contains(".");
            String url = "https://api.themoviedb.org/3/find/" + item.id + "?external_source=imdb_id";
            if (!jwt) url += "&api_key=" + credential;
            JSONObject response = NetJson.get(url,jwt ? credential : "");
            JSONArray results = response.optJSONArray(item.type.equals("series") ? "tv_results" : "movie_results");
            JSONObject first = results == null ? null : results.optJSONObject(0);
            return first == null ? "" : first.optString("backdrop_path","");
        } catch (IOException error) { return ""; }
    }
    private String cinemeta(ResumeItem item) throws IOException {
        JSONObject response = NetJson.get("https://v3-cinemeta.strem.io/meta/" + item.type + "/" + item.id + ".json","");
        JSONObject meta = response.optJSONObject("meta");
        return meta == null ? "" : meta.optString("background","");
    }
}
