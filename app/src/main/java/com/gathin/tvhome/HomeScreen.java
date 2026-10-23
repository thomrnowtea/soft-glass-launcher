package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Coordinates native sections and lifecycle. Network data never controls Home navigation. */
final class HomeScreen implements HeroSection.Listener {
    final ScrollView root;
    final String styleSignature;
    private final Activity activity;
    private final TvPreferences preferences;
    private final TvPalette palette;
    private final AppCatalog catalog;
    private final ArtworkCache artwork;
    private final TmdbRepository movies;
    private final WeatherRepository weather;
    private final HeroSection hero;
    private final DockSection dock;
    private final HomeStatusSection status;
    private final ExecutorService requests = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable weatherTick = () -> loadWeather();
    private final List<TextView> tabs = new ArrayList<>();
    private boolean visible, opening;
    private int generation;

    HomeScreen(Activity activity) {
        this.activity = activity; preferences = new TvPreferences(activity); palette = TvPalette.of(activity);
        styleSignature = style(activity); catalog = new AppCatalog(activity); artwork = new ArtworkCache(activity);
        movies = new TmdbRepository(activity); weather = new WeatherRepository(activity);
        root = new ScrollView(activity); root.setFillViewport(true); root.setVerticalScrollBarEnabled(false);
        root.setBackgroundColor(palette.background); root.setClipChildren(false); root.setClipToPadding(false);
        LinearLayout page = TvUi.column(activity); page.setPadding(dp(40), dp(22), dp(40), dp(30)); root.addView(page);
        chrome(page); categoryTabs(page); TvUi.gap(page, 10);
        hero = new HeroSection(activity, artwork, this); page.addView(hero.root); TvUi.gap(page, 18);
        dock = new DockSection(activity); page.addView(dock.root); TvUi.gap(page, 20);
        status = new HomeStatusSection(activity); page.addView(status.root);
        status.show(weather.cached()); hero.show(movies.cached());
    }
    static String style(Activity activity) {
        TvPreferences value = new TvPreferences(activity);
        return value.theme() + ":" + TvPalette.of(activity).light + ":" + value.motion() + ":" + value.reduced();
    }
    private void chrome(LinearLayout page) {
        LinearLayout bar = TvUi.row(activity);
        TextClock clock = new TextClock(activity); clock.setFormat12Hour("h:mm"); clock.setFormat24Hour("HH:mm");
        clock.setTextSize(43); clock.setTextColor(palette.text); clock.setTypeface(activity.getResources().getFont(R.font.manrope_light));
        clock.setIncludeFontPadding(false); clock.setLetterSpacing(-.04f); bar.addView(clock, TvUi.params(activity, 136, 56, 16));
        LinearLayout greeting = TvUi.column(activity);
        greeting.addView(TvUi.title(activity, TvUi.label(activity, "Welcome home", "Bienvenido a casa"), 17, palette.text));
        TextClock date = new TextClock(activity); date.setFormat12Hour("EEEE d MMMM"); date.setFormat24Hour("EEEE d MMMM");
        date.setTextSize(12); date.setTextColor(palette.muted); date.setSingleLine(true); greeting.addView(date);
        bar.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        nav(bar, TvUi.label(activity, "Apps", "Apps"), () -> openActivity(AppsActivity.class));
        nav(bar, TvUi.label(activity, "Inputs", "Entradas"), this::inputs);
        nav(bar, "Wi-Fi", () -> safe(new Intent(Settings.ACTION_WIFI_SETTINGS)));
        nav(bar, TvUi.label(activity, "Settings", "Ajustes"), this::settings);
        page.addView(bar);
    }
    private void nav(LinearLayout row, String label, Runnable action) {
        row.addView(TvUi.button(activity, label, palette, action), TvUi.params(activity, -2, 40, 8));
    }
    private void categoryTabs(LinearLayout page) {
        TvUi.gap(page, 12); LinearLayout row = TvUi.row(activity);
        String[] keys = {"all", "movie", "tv"};
        String[] labels = {TvUi.label(activity, "For you", "Para vos"), TvUi.label(activity, "Movies", "Películas"), TvUi.label(activity, "Series", "Series")};
        for (int index = 0; index < keys.length; index++) {
            String key = keys[index];
            TextView button = TvUi.button(activity, labels[index], palette, () -> { preferences.text("tab", key); updateTabs(); loadMovies(false); });
            button.setTag(key); tabs.add(button); row.addView(button, TvUi.params(activity, -2, 34, 8));
        }
        page.addView(row); updateTabs();
    }
    private void updateTabs() {
        for (TextView tab : tabs) tab.setTextColor(preferences.tab().equals(tab.getTag()) ? palette.accent : palette.muted);
    }
    void resume() {
        visible = true; opening = false; updateTabs();
        catalog.reload(); dock.show(catalog.favorites(), catalog::launch, this::settings);
        status.refreshLights(); loadWeather(); loadMovies(false);
        if (root.findFocus() == null) root.post(dock::restoreFocus);
    }
    void pause() { visible = false; generation++; main.removeCallbacksAndMessages(null); }
    void close() { pause(); requests.shutdownNow(); artwork.close(); }
    void home() { root.scrollTo(0, 0); root.post(dock::focusFirst); }
    @Override public void settings() { openActivity(LauncherSettingsActivity.class); }
    private void openActivity(Class<?> target) { activity.startActivity(new Intent(activity, target)); }

    private void loadWeather() {
        int request = generation;
        requests.execute(() -> {
            WeatherData data;
            try { data = weather.load(false); }
            catch (IOException error) { data = weather.cached(); }
            WeatherData result = data;
            main.post(() -> {
                if (!visible || generation != request) return;
                status.show(result); main.removeCallbacks(weatherTick);
                main.postDelayed(weatherTick, WeatherRepository.MAX_AGE_MS);
            });
        });
    }
    private void loadMovies(boolean force) {
        if (!visible) return;
        int request = generation; String profile = preferences.signature();
        if (!movies.configured()) {
            hero.message(TvUi.label(activity, "A little inspiration", "Algo para descubrir"),
                    TvUi.label(activity, "Connect TMDB in Settings for your movie and series picks.", "Conectá TMDB en Ajustes para ver películas y series elegidas para vos.")); return;
        }
        requests.execute(() -> {
            List<Movie> result;
            boolean offline = false;
            try { result = movies.load(force); }
            catch (IOException error) { result = movies.cached(); offline = true; }
            List<Movie> data = result;
            boolean saved = offline;
            main.post(() -> {
                if (!visible || generation != request || !preferences.signature().equals(profile)) return;
                hero.show(data);
                hero.status(recommendationStatus(saved));
            });
        });
    }
    private String recommendationStatus(boolean offline) {
        if (offline) return TvUi.label(activity, "SAVED PICKS · OFFLINE", "GUARDADO · SIN CONEXIÓN");
        boolean personalized = !preferences.genres().isEmpty() || !preferences.seed().isEmpty();
        return TvUi.label(activity, personalized ? "CHOSEN FOR YOU" : "TRENDING THIS WEEK",
                personalized ? "ELEGIDO PARA VOS" : "TENDENCIAS DE LA SEMANA");
    }
    @Override public void open(Movie movie) {
        if (opening) return;
        opening = true; hero.opening(true); int request = generation;
        requests.execute(() -> {
            String imdb;
            try { imdb = movies.imdb(movie); }
            catch (IOException error) { main.post(() -> cannotOpen(request)); return; }
            main.post(() -> {
                if (!visible || generation != request) return;
                opening = false; hero.opening(false);
                safe(new Intent(Intent.ACTION_VIEW, Uri.parse("stremio:///detail/" + (movie.type.equals("tv") ? "series" : "movie") + "/" + imdb))
                        .setPackage("com.stremio.one"));
            });
        });
    }
    private void cannotOpen(int request) {
        if (!visible || generation != request) return;
        opening = false; hero.opening(false);
        Toast.makeText(activity, TvUi.label(activity, "Could not open this title. Check the connection and try again.",
                "No se pudo abrir este título. Revisá la conexión y reintentá."), Toast.LENGTH_LONG).show();
    }
    @Override public void options(Movie movie) {
        String[] choices = {TvUi.label(activity, "More like this", "Más como este"), TvUi.label(activity, "Not interested", "No me interesa"),
                TvUi.label(activity, "Open in Stremio", "Abrir en Stremio")};
        TvUi.dialog(activity).setTitle(movie.title).setItems(choices, (dialog, choice) -> option(movie, choice)).show();
    }
    private void option(Movie movie, int choice) {
        if (choice == 2) { open(movie); return; }
        if (choice == 0) preferences.text("seed", movie.key());
        else preferences.hide(movie.key());
        loadMovies(choice == 0);
    }
    void inputs() {
        TvInputManager manager = activity.getSystemService(TvInputManager.class);
        List<TvInputInfo> devices = manager == null ? new ArrayList<>() : manager.getTvInputList();
        List<String> names = new ArrayList<>(); List<Intent> actions = new ArrayList<>();
        for (TvInputInfo device : devices) {
            if (!device.isPassthroughInput() || device.getParentId() != null) continue;
            names.add(String.valueOf(device.loadLabel(activity)));
            actions.add(new Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.buildChannelUriForPassthroughInput(device.getId()), "vnd.android.cursor.item/channel"));
        }
        Intent live = activity.getPackageManager().getLeanbackLaunchIntentForPackage("com.tcl.tv");
        if (live != null) { names.add(TvUi.label(activity, "Live TV", "TV en vivo")); actions.add(live); }
        if (names.isEmpty()) { safe(new Intent(Settings.ACTION_SETTINGS)); return; }
        TvUi.dialog(activity).setTitle(TvUi.label(activity, "Inputs", "Entradas"))
                .setItems(names.toArray(new String[0]), (dialog, selected) -> safe(actions.get(selected))).show();
    }
    private void safe(Intent intent) {
        try { activity.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (ActivityNotFoundException error) { Toast.makeText(activity, R.string.not_available, Toast.LENGTH_SHORT).show(); }
    }
    boolean key(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_SETTINGS || key == KeyEvent.KEYCODE_NOTIFICATION) {
            if (event.getAction() == KeyEvent.ACTION_UP) settings(); return true;
        }
        if (key == KeyEvent.KEYCODE_TV_INPUT) {
            if (event.getAction() == KeyEvent.ACTION_UP) inputs(); return true;
        }
        return false;
    }
    private int dp(int value) { return TvUi.dp(activity, value); }
}
