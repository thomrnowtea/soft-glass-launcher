package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class TmdbRepository {
    private static final long TTL = 6 * 60 * 60_000L;
    private final Context context;
    private final TvPreferences preferences;
    private final SharedPreferences cache;
    private final TmdbAuth auth;
    TmdbRepository(Context context) {
        this.context = context.getApplicationContext();
        preferences = new TvPreferences(context);
        cache = context.getSharedPreferences("tmdb_catalogue_cache", Context.MODE_PRIVATE);
        auth = new TmdbAuth(context);
    }
    boolean configured() { return auth.configured(); }
    List<Movie> cached() {
        try { return decode(new JSONArray(cache.getString("items", "[]")), "movie"); }
        catch (JSONException error) { return new ArrayList<>(); }
    }
    List<Movie> load(boolean force) throws IOException {
        if (!auth.configured()) return new ArrayList<>();
        long age = System.currentTimeMillis() - cache.getLong("saved", 0);
        if (!force && preferences.signature().equals(cache.getString("profile", "")) && age >= 0 && age < TTL)
            return cached();
        List<Movie> items = personalized();
        JSONArray stored = new JSONArray();
        for (Movie movie : items) stored.put(movie.json());
        cache.edit().putString("items", stored.toString()).putString("profile", preferences.signature())
                .putLong("saved", System.currentTimeMillis()).apply();
        return items;
    }
    private List<Movie> personalized() throws IOException {
        String tab = preferences.tab();
        String seed = preferences.seed();
        if (seed.matches("(movie|tv):[0-9]+") && (tab.equals("all") || seed.startsWith(tab + ":"))) {
            String[] parts = seed.split(":");
            List<Movie> related = results(get("/" + parts[0] + "/" + parts[1] + "/recommendations", ""), parts[0]);
            if (!related.isEmpty()) return related;
        }
        if (preferences.genres().isEmpty()) return results(get("/trending/" + tab + "/week", ""), tab);
        List<Movie> result = new ArrayList<>();
        if (!tab.equals("tv")) result.addAll(discover("movie"));
        if (!tab.equals("movie")) result.addAll(discover("tv"));
        result.sort((a, b) -> Double.compare(b.rating, a.rating));
        return result;
    }
    private List<Movie> discover(String type) throws IOException {
        String genres = preferences.genres();
        if (type.equals("tv")) genres = genres.replace("28", "10759").replace("878", "10765").replace("53", "9648");
        return results(get("/discover/" + type, "&sort_by=popularity.desc&vote_count.gte=50&with_genres="
                + encode(genres.replace(',', '|'))), type);
    }
    private JSONObject get(String route, String query) throws IOException {
        String key = auth.read();
        String endpoint = "https://api.themoviedb.org/3" + route
                + "?language=" + encode(language()) + region() + "&include_adult=false" + query;
        String bearer = key;
        if (key.matches("[a-fA-F0-9]{32}")) { endpoint += "&api_key=" + key; bearer = ""; }
        return NetJson.get(endpoint, bearer);
    }
    private String language() {
        return context.getResources().getConfiguration().getLocales().get(0).toLanguageTag();
    }
    private String region() {
        String country=context.getResources().getConfiguration().getLocales().get(0).getCountry();
        return country.matches("[A-Z]{2}") ? "&region="+country : "";
    }
    private List<Movie> results(JSONObject response, String fallback) {
        JSONArray data = response.optJSONArray("results");
        return data == null ? new ArrayList<>() : decode(data, fallback);
    }
    private List<Movie> decode(JSONArray data, String fallback) {
        List<Movie> result = new ArrayList<>();
        Set<String> hidden = preferences.hiddenTitles();
        for (int index = 0; index < data.length() && result.size() < 16; index++) {
            try {
                Movie movie = new Movie(data.getJSONObject(index), fallback);
                if (!hidden.contains(movie.key()) && !movie.title.isEmpty()) result.add(movie);
            } catch (JSONException ignored) { /* People and incomplete entries do not become movie tiles. */ }
        }
        return result;
    }
    String imdb(Movie movie) throws IOException {
        String key = "imdb_" + movie.key();
        String saved = cache.getString(key, "");
        if (saved.matches("tt[0-9]+")) return saved;
        String found = get("/" + movie.type + "/" + movie.id + "/external_ids", "").optString("imdb_id", "");
        if (!found.matches("tt[0-9]+")) throw new IOException("No IMDb link");
        cache.edit().putString(key, found).apply();
        return found;
    }
    private static String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8"); }
        catch (UnsupportedEncodingException impossible) { throw new IllegalStateException(impossible); }
    }
}
