package com.gathin.tvhome;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class WeatherData {
    static final ZoneId ZONE = ZoneId.systemDefault();
    final java.time.ZoneId zone;
    final JSONObject json;
    final long fetchedAt;
    final double temperature;
    final int code;
    final String observedTime;

    WeatherData(JSONObject json, long fetchedAt) throws JSONException {
        JSONObject current = json.getJSONObject("current");
        temperature = current.getDouble("temperature_2m");
        code = current.getInt("weather_code");
        observedTime = current.getString("time");
        LocalDateTime.parse(observedTime);
        if (!Double.isFinite(temperature)) throw new IllegalArgumentException("Missing temperature");
        json.getJSONObject("daily").getJSONArray("time");
        zone=ZoneId.of(json.optString("timezone",ZONE.getId()));
        this.json = json;
        this.fetchedAt = fetchedAt;
    }

    boolean fresh() {
        long age = System.currentTimeMillis() - fetchedAt;
        return age >= 0 && age < WeatherRepository.MAX_AGE_MS;
    }

    String updated(Context context) {
        String time = Instant.ofEpochMilli(fetchedAt).atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
        return context.getString(fresh() ? R.string.hc_updated : R.string.hc_cached, time);
    }

    String compact(Context context) {
        return context.getString(R.string.hc_weather_compact, Math.round(temperature),
                description(context, code), daySummary(context, 0));
    }

    String daySummary(Context context, int index) {
        JSONObject daily = json.optJSONObject("daily");
        return context.getString(R.string.hc_day_summary,
                number(daily, "temperature_2m_min", index, "°"),
                number(daily, "temperature_2m_max", index, "°"),
                number(daily, "precipitation_probability_max", index, "%"));
    }

    String dayTitle(int index) {
        String date = json.optJSONObject("daily").optJSONArray("time").optString(index);
        try {
            return LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.getDefault()));
        } catch (RuntimeException error) {
            return date;
        }
    }

    String hourly(Context context) {
        JSONObject hourly = json.optJSONObject("hourly");
        if (hourly == null) return context.getString(R.string.hc_unavailable);
        JSONArray times = hourly.optJSONArray("time");
        if (times == null) return context.getString(R.string.hc_unavailable);
        String start = LocalDateTime.now(zone).withMinute(0).withSecond(0).withNano(0).toString();
        StringBuilder text = new StringBuilder();
        int rows = 0;
        for (int index = 0; index < times.length() && rows < 6; index++) {
            String time = times.optString(index);
            if (time.compareTo(start) < 0) continue;
            text.append(time.replace('T', ' ')).append("   ")
                    .append(number(hourly, "temperature_2m", index, " °C"))
                    .append("   ").append(context.getString(R.string.hc_rain))
                    .append(' ').append(number(hourly, "precipitation_probability", index, "%"))
                    .append('\n');
            rows++;
        }
        return text.toString().trim();
    }

    static String number(JSONObject object, String key, int index, String unit) {
        if (object == null) return "—";
        JSONArray values = object.optJSONArray(key);
        if (values == null || values.isNull(index)) return "—";
        double value = values.optDouble(index, Double.NaN);
        return Double.isFinite(value) ? Math.round(value) + unit : "—";
    }

    static String description(Context context, int code) {
        if (code == 0) return context.getString(R.string.hc_clear);
        if (code <= 3) return context.getString(R.string.hc_cloudy);
        if (code <= 48) return context.getString(R.string.hc_fog);
        if (code <= 67) return context.getString(R.string.hc_rain);
        if (code <= 77) return context.getString(R.string.hc_snow);
        if (code <= 86) return context.getString(R.string.hc_showers);
        return context.getString(R.string.hc_storm);
    }
}
