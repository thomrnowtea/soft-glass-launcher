package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owner choices remain separate from upstream themes and from the private API credential. */
final class TvPreferences {
    private final SharedPreferences values;
    TvPreferences(Context context) { values = context.getSharedPreferences("living_room", Context.MODE_PRIVATE); }
    String theme() { return values.getString("theme", "dark"); }
    boolean motion() { return values.getBoolean("motion", true); }
    boolean reduced() { return values.getBoolean("reduced", false); }
    boolean widget(HomeWidget widget) { return values.getBoolean(widget.key(),true); }
    void widget(HomeWidget widget,boolean visible) { flag(widget.key(),visible); }
    String widgetSignature() {
        StringBuilder signature=new StringBuilder();
        for (HomeWidget widget:HomeWidget.values()) signature.append(widget(widget) ? '1' : '0');
        return signature.toString();
    }
    boolean anyWidget() {
        for (HomeWidget widget:HomeWidget.values()) if (widget(widget)) return true;
        return false;
    }
    String voiceLanguage() { return values.getString("voice_language",java.util.Locale.getDefault().toLanguageTag()); }
    String tab() { return values.getString("tab", "all"); }
    String genres() { return values.getString("genres", ""); }
    String seed() { return values.getString("seed", ""); }
    int dockSize() { return Math.max(4, Math.min(8, values.getInt("dock_size", 4))); }
    String signature() { return tab() + ":" + genres() + ":" + seed(); }
    void text(String key, String value) { values.edit().putString(key, value).apply(); }
    void flag(String key, boolean value) { values.edit().putBoolean(key, value).apply(); }
    void number(String key, int value) { values.edit().putInt(key, value).apply(); }
    Set<String> hiddenTitles() { return new HashSet<>(values.getStringSet("hidden_titles", new HashSet<>())); }
    void hide(String key) {
        Set<String> hidden = hiddenTitles(); hidden.add(key);
        values.edit().putStringSet("hidden_titles", hidden).apply();
    }
    void resetTitles() { values.edit().remove("seed").remove("hidden_titles").apply(); }
    List<String> favorites(List<String> defaults) {
        String saved = values.getString("favorites", null);
        if (saved == null) return new ArrayList<>(defaults);
        List<String> result = new ArrayList<>();
        try {
            JSONArray list = new JSONArray(saved);
            for (int index = 0; index < Math.min(200, list.length()); index++) {
                String pkg=list.optString(index);
                if (!pkg.isEmpty() && !result.contains(pkg)) result.add(pkg);
            }
        } catch (JSONException error) { return new ArrayList<>(defaults); }
        return result;
    }
    /** Existing favorites become the leading order; all other apps join unless explicitly hidden. */
    List<String> homeOrder(List<String> defaults,List<String> installed) {
        List<String> previous=favorites(defaults),order=new ArrayList<>(previous);
        Set<String> hidden=homeHidden(); order.removeAll(hidden);
        for (String pkg:installed) if (!hidden.contains(pkg) && !order.contains(pkg)) order.add(pkg);
        if (!order.equals(previous)) saveFavorites(order);
        return order;
    }
    private Set<String> homeHidden() { return new HashSet<>(values.getStringSet("home_hidden",new HashSet<>())); }
    boolean homeAppVisible(String pkg) { return !homeHidden().contains(pkg); }
    void hideHomeApp(String pkg,List<String> defaults) {
        Set<String> hidden=homeHidden(); hidden.add(pkg);
        List<String> order=favorites(defaults); order.remove(pkg);
        values.edit().putStringSet("home_hidden",hidden).putString("favorites",new JSONArray(order).toString()).apply();
    }
    void showHomeApp(String pkg,List<String> defaults) {
        Set<String> hidden=homeHidden(); hidden.remove(pkg);
        List<String> order=favorites(defaults); if (!order.contains(pkg)) order.add(pkg);
        values.edit().putStringSet("home_hidden",hidden).putString("favorites",new JSONArray(order).toString()).apply();
    }
    void saveFavorites(List<String> apps) { text("favorites",new JSONArray(apps).toString()); }
    boolean hasHomeSelection() { return values.contains("favorites"); }
    void saveHomeSelection(List<String> apps,Set<String> hidden) {
        values.edit().putString("favorites",new JSONArray(apps).toString()).putStringSet("home_hidden",hidden).apply();
    }
    void favorite(int position, String pkg, List<String> defaults) {
        List<String> list = favorites(defaults);
        while (list.size() <= position) list.add("");
        int previous = list.indexOf(pkg);
        if (previous >= 0 && previous != position) list.set(previous, list.get(position));
        list.set(position, pkg);
        text("favorites", new JSONArray(list).toString());
    }
}
