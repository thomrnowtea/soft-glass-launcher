package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/** First-run state belongs to this installation, independently of appearance or account data. */
final class SetupState {
    private static SharedPreferences state(Context context) { return context.getSharedPreferences("softglass_setup",0); }
    static void initialize(Context context) {
        SharedPreferences setup=state(context);
        if (setup.contains("initialized")) return;
        boolean existing=hasExistingChoices(context);
        if (existing) {
            // Earlier releases used Palermo implicitly. Materialize only that legacy default.
            if (BuildConfig.APPLICATION_ID.equals("com.gathin.tvhome") && !WeatherLocation.configured(context)) WeatherLocation.palermo().save(context);
        } else {
            TvPreferences preferences=new TvPreferences(context);
            preferences.text("theme","system");
            preferences.widget(HomeWidget.WEATHER,false);
            preferences.widget(HomeWidget.LIGHTS,false);
            preferences.widget(HomeWidget.INPUTS,!TvInputs.list(context).isEmpty());
        }
        setup.edit().putBoolean("initialized",true).putBoolean("complete",existing).putBoolean("existing",existing).apply();
    }
    private static boolean hasExistingChoices(Context context) {
        return !context.getSharedPreferences("living_room",0).getAll().isEmpty()
                || new StremioStore(context).linked() || !new WizStore(context).load().isEmpty()
                || new TmdbAuth(context).configured();
    }
    static boolean needed(Context context) { return !state(context).getBoolean("complete",false); }
    static int step(Context context) { return Math.max(0,Math.min(5,state(context).getInt("step",0))); }
    static void step(Context context,int step) { state(context).edit().putInt("step",step).apply(); }
    static void complete(Context context) { state(context).edit().putBoolean("complete",true).putInt("step",0).apply(); }
    static void revisit(Context context) { state(context).edit().putInt("step",0).remove("apps").apply(); }
    static List<String> selected(Context context,List<AppAdapter.Entry> installed) {
        String draft=state(context).getString("apps",null);
        if (draft!=null) return decode(draft);
        List<String> selected=new ArrayList<>();
        TvPreferences preferences=new TvPreferences(context);
        List<String> previous=preferences.favorites(Config.load(context).dockApps);
        for (String pkg:previous) if (contains(installed,pkg) && preferences.homeAppVisible(pkg)) selected.add(pkg);
        if (selected.isEmpty() && !preferences.hasHomeSelection()) {
            for (AppAdapter.Entry app:installed) { selected.add(app.pkg); if (selected.size()==6) break; }
        }
        saveSelection(context,selected); return selected;
    }
    private static boolean contains(List<AppAdapter.Entry> apps,String pkg) {
        for (AppAdapter.Entry app:apps) if (app.pkg.equals(pkg)) return true;
        return false;
    }
    private static List<String> decode(String value) {
        List<String> result=new ArrayList<>();
        try {
            JSONArray list=new JSONArray(value);
            for (int i=0;i<Math.min(200,list.length());i++) {
                String pkg=list.optString(i); if (!pkg.isEmpty() && !result.contains(pkg)) result.add(pkg);
            }
        } catch (JSONException invalid) { /* An invalid draft never overwrites the Home selection. */ }
        return result;
    }
    static void saveSelection(Context context,List<String> selected) {
        state(context).edit().putString("apps",new JSONArray(selected).toString()).apply();
    }
    static void applySelection(Context context,List<String> selected,List<AppAdapter.Entry> installed) {
        HashSet<String> hidden=new HashSet<>();
        for (AppAdapter.Entry app:installed) if (!selected.contains(app.pkg)) hidden.add(app.pkg);
        new TvPreferences(context).saveHomeSelection(selected,hidden);
    }
}
