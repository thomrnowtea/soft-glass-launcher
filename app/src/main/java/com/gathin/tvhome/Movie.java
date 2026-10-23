package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

final class Movie {
    final int id;
    final String type, title, overview, poster, backdrop, date;
    final double rating;
    Movie(JSONObject json, String fallback) throws JSONException {
        id = json.getInt("id");
        type = json.optString("media_type", fallback);
        if (!type.equals("movie") && !type.equals("tv")) throw new JSONException("Not a movie or series");
        title = json.optString("title", json.optString("name", ""));
        overview = json.optString("overview", "");
        poster = json.optString("poster_path", ""); backdrop = json.optString("backdrop_path", "");
        date = json.optString("release_date", json.optString("first_air_date", ""));
        rating = json.optDouble("vote_average", 0);
    }
    String key() { return type + ":" + id; }
    String year() { return date.length() >= 4 ? date.substring(0, 4) : ""; }
    String metadata() { return (type.equals("movie") ? "MOVIE" : "SERIES") + "  ·  " + year()
            + "  ·  ★ " + String.format(Locale.getDefault(), "%.1f", rating); }
    JSONObject json() {
        try {
            return new JSONObject().put("id", id).put("media_type", type).put("title", title)
                    .put("overview", overview).put("poster_path", poster).put("backdrop_path", backdrop)
                    .put("release_date", date).put("vote_average", rating);
        } catch (JSONException impossible) { throw new IllegalStateException(impossible); }
    }
}
