package com.gathin.tvhome;

import android.content.Context;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Shares upstream discovery, removals, label cleaning, order and launch semantics. */
final class AppCatalog {
    private final Context context;
    private AppAdapter source;
    private List<AppAdapter.Entry> entries = new ArrayList<>();
    AppCatalog(Context context) { this.context = context; }
    List<AppAdapter.Entry> reload() {
        Config config = Config.load(context);
        source = new AppAdapter(context, NO_FOCUS_RENDERER, config.columns, config);
        List<String> favorites = new TvPreferences(context).favorites(config.dockApps);
        source.setSavedOrder(Order.ranks(Order.load(Order.file(context)), favorites));
        source.reload(Collections.emptyList());
        entries = source.catalogEntries();
        return new ArrayList<>(entries);
    }
    List<AppAdapter.Entry> favorites() {
        List<String> installed=new ArrayList<>();
        for (AppAdapter.Entry entry:entries) installed.add(entry.pkg);
        List<String> desired = new TvPreferences(context).homeOrder(Config.load(context).dockApps,installed);
        List<AppAdapter.Entry> result = new ArrayList<>();
        for (String pkg : desired) for (AppAdapter.Entry entry : entries) if (entry.pkg.equals(pkg)) { result.add(entry); break; }
        return result;
    }
    void launch(AppAdapter.Entry entry) { source.launch(entry); }
    private static final AppAdapter.TileListener NO_FOCUS_RENDERER = new AppAdapter.TileListener() {
        @Override public void onTileFocused(int position, View card) { }
        @Override public boolean onTileVerticalKey(int position, int key) { return false; }
        @Override public void onLibraryRequested() { }
        @Override public void onLibraryFocused() { }
        @Override public void onLibraryUp() { }
    };
}
