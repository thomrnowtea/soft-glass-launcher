package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;
import java.io.IOException;

/** Forecast and cache are keyed to the selected place; no GPS or background work. */
final class WeatherRepository {
    static final long MAX_AGE_MS=30*60_000L;
    private final Context context;
    private final SharedPreferences preferences;
    WeatherRepository(Context context) {
        this.context=context;
        preferences=context.getSharedPreferences(BuildConfig.APPLICATION_ID.equals("com.gathin.tvhome") ? "palermo_weather" : "weather_cache",0);
    }
    WeatherData cached() {
        if (!WeatherLocation.configured(context)) return null;
        String place=WeatherLocation.load(context).signature();
        if (!place.equals(preferences.getString("place",""))) return null;
        try { return new WeatherData(new JSONObject(preferences.getString("json","")),preferences.getLong("fetched",0)); }
        catch (Exception invalid) { return null; }
    }
    WeatherData load(boolean force) throws IOException {
        if (!WeatherLocation.configured(context)) throw new IOException("No forecast location selected");
        WeatherData cached=cached();
        if (!force && cached!=null && cached.fresh()) return cached;
        WeatherLocation place=WeatherLocation.load(context);
        String endpoint="https://api.open-meteo.com/v1/forecast?latitude="+place.latitude+"&longitude="+place.longitude
                +"&current=temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m"
                +"&hourly=temperature_2m,precipitation_probability,weather_code"
                +"&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                +"&forecast_days=3&timezone="+Uri.encode(place.timezone);
        try {
            JSONObject json=NetJson.get(endpoint,""); WeatherData data=new WeatherData(json,System.currentTimeMillis());
            if (place.signature().equals(WeatherLocation.load(context).signature())) preferences.edit().putString("json",json.toString())
                    .putLong("fetched",data.fetchedAt).putString("place",place.signature()).apply();
            return data;
        } catch (org.json.JSONException | IllegalArgumentException | java.time.DateTimeException invalid) {
            throw new IOException("Invalid forecast",invalid);
        }
    }
}
