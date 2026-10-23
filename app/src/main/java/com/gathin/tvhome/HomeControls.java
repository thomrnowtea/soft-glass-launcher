package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Home owns its timer. Pausing it cancels the timer; there is no periodic service. */
final class HomeControls {
    private final Activity activity;
    private final WeatherRepository repository;
    private final TextView forecast;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService network = Executors.newSingleThreadExecutor();
    private final Runnable refreshTick = this::refresh;
    private Future<?> pending;
    private boolean visible;
    private int generation;

    HomeControls(Activity activity, Runnable focusDock) {
        this.activity = activity;
        repository = new WeatherRepository(activity);
        forecast = activity.findViewById(R.id.homeForecast);
        View weather = activity.findViewById(R.id.homeWeather);
        View lights = activity.findViewById(R.id.homeLights);
        weather.setOnClickListener(view -> activity.startActivity(new Intent(activity, WeatherActivity.class)));
        lights.setOnClickListener(view -> activity.startActivity(new Intent(activity, LightsActivity.class)));
        View.OnKeyListener keys = (view, key, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN || key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
            focusDock.run();
            return true;
        };
        weather.setOnKeyListener(keys);
        lights.setOnKeyListener(keys);
    }

    void resume() {
        visible = true;
        TextView lights = activity.findViewById(R.id.homeLightsLabel);
        lights.setText(activity.getString(R.string.hc_lights_home, new WizStore(activity).load().size()));
        show(repository.cached(), false);
        refresh();
    }

    void pause() {
        visible = false;
        generation++;
        main.removeCallbacksAndMessages(null);
        if (pending != null) pending.cancel(true);
    }

    void close() { pause(); network.shutdownNow(); }

    private void refresh() {
        if (!visible) return;
        int request = generation;
        pending = network.submit(() -> {
            WeatherData data;
            boolean offline = false;
            try { data = repository.load(false); }
            catch (IOException error) { data = repository.cached(); offline = true; }
            WeatherData result = data;
            boolean failed = offline;
            main.post(() -> {
                if (!visible || generation != request) return;
                show(result, failed);
                main.removeCallbacks(refreshTick);
                main.postDelayed(refreshTick, nextDelay(result));
            });
        });
    }

    private long nextDelay(WeatherData data) {
        if (data == null || !data.fresh()) return WeatherRepository.MAX_AGE_MS;
        return Math.max(1000, WeatherRepository.MAX_AGE_MS - (System.currentTimeMillis() - data.fetchedAt));
    }

    private void show(WeatherData data, boolean offline) {
        if (data == null) { forecast.setText(R.string.hc_unavailable); return; }
        String text = data.compact(activity);
        if (offline || !data.fresh()) text += " · " + activity.getString(R.string.hc_offline);
        forecast.setText(text);
        ((TextView) activity.findViewById(R.id.homeWeatherLabel)).setText(
                "Palermo · " + data.updated(activity));
        activity.findViewById(R.id.homeWeather).setContentDescription(
                activity.getString(R.string.hc_weather_title) + " · " + text + " · " + data.updated(activity));
        Log.i("HomeControls", "home weather rendered fresh=" + data.fresh() + " offline=" + offline);
    }
}
