package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HomeStatusSection {
    final LinearLayout root;
    final ForecastCard weather;
    private final TextView lightCount;
    private final Activity activity;
    HomeStatusSection(Activity activity) {
        this.activity = activity; TvPalette palette = TvPalette.of(activity);
        root = TvUi.row(activity);
        weather = new ForecastCard(activity, true); TvUi.focus(weather, palette, 22, null);
        weather.setOnClickListener(view -> activity.startActivity(new Intent(activity, WeatherActivity.class)));
        LinearLayout.LayoutParams forecast = new LinearLayout.LayoutParams(0, TvUi.dp(activity, 190), 2);
        forecast.setMarginEnd(TvUi.dp(activity, 14)); root.addView(weather, forecast);
        LinearLayout lights = TvUi.column(activity); lights.setPadding(TvUi.dp(activity, 22), TvUi.dp(activity, 20), TvUi.dp(activity, 22), TvUi.dp(activity, 20));
        TvUi.surface(lights, palette, 22); TvUi.focus(lights, palette, 22, null);
        lights.addView(TvUi.title(activity, "WiZ", 27, palette.text)); TvUi.gap(lights, 8);
        lightCount = TvUi.text(activity, "", 15, palette.muted); lights.addView(lightCount);
        TvUi.gap(lights, 20);
        lights.addView(TvUi.title(activity, TvUi.label(activity, "Power controls  →", "Encendido y apagado  →"), 14, palette.accent));
        lights.setOnClickListener(view -> activity.startActivity(new Intent(activity, LightsActivity.class)));
        root.addView(lights, new LinearLayout.LayoutParams(0, TvUi.dp(activity, 190), 1));
    }
    void show(WeatherData data) { weather.show(data); }
    void refreshLights() {
        int count = new WizStore(activity).load().size();
        lightCount.setText(activity.getString(R.string.hc_lights_home, count));
    }
}
