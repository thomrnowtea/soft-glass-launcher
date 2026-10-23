package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LivingApps {
    final LinearLayout root;
    private final Activity activity;
    private final LinearLayout grid;
    private View first;
    LivingApps(Activity activity) {
        this.activity=activity; root=TvUi.column(activity);
        LinearLayout header=TvUi.row(activity); TvPalette palette=TvPalette.of(activity);
        android.widget.TextView heading=TvUi.title(activity,TvUi.label(activity,"More apps","Más apps"),18,palette.text); TvAccessibility.heading(heading);
        header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(TvUi.plain(activity,TvUi.label(activity,"Edit featured","Editar destacadas"),
                () -> activity.startActivity(new Intent(activity,FavoritesActivity.class))));
        root.addView(header,TvUi.params(activity,-1,-2,0)); TvUi.gap(root,12);
        grid=TvUi.column(activity); root.addView(grid);
        root.addView(TvUi.plain(activity,activity.getString(R.string.lr_all_apps),
                () -> activity.startActivity(new Intent(activity,AppsActivity.class))));
    }
    void render(List<AppAdapter.Entry> apps,List<AppAdapter.Entry> featured,AppCatalog catalog) {
        grid.removeAllViews(); first=null;
        Set<String> favorites=new HashSet<>(); for (AppAdapter.Entry app:featured) favorites.add(app.pkg);
        int screen=Math.round(activity.getResources().getDisplayMetrics().widthPixels/activity.getResources().getDisplayMetrics().density);
        int width=Math.max(100,(screen-104-48)/4);
        LinearLayout row=null; int count=0;
        for (AppAdapter.Entry app:apps) {
            if (favorites.contains(app.pkg)) continue;
            if (count>=8) break;
            if (count%4==0) { row=TvUi.row(activity); grid.addView(row); TvUi.gap(grid,16); }
            AppTile tile=new AppTile(activity,app,width,95,true,() -> catalog.launch(app));
            tile.setOnLongClickListener(v -> { activity.startActivity(new Intent(activity,FavoritesActivity.class)); return true; });
            row.addView(tile,TvUi.params(activity,width,-2,16)); if (first==null) first=tile; count++;
        }
        if (first==null) grid.addView(TvUi.text(activity,TvUi.label(activity,"No other apps","No hay otras apps"),16,TvPalette.of(activity).muted));
    }
    void focus() { if (first!=null) first.requestFocus(); else root.requestFocus(View.FOCUS_FORWARD); }
    boolean topRow(View view) { return LivingHome.within(view,grid.getChildAt(0)) || LivingHome.within(view,root.getChildAt(0)); }
    void navigation(java.util.List<FocusNavigator.Row> rows) {
        FocusNavigator.add(rows,"app-tools",root.getChildAt(0)); int index=0;
        for (int i=0;i<grid.getChildCount();i++) {
            View row=grid.getChildAt(i);
            if (row instanceof LinearLayout) FocusNavigator.add(rows,"app-grid-"+(index++),row);
        }
        FocusNavigator.add(rows,"app-footer",root.getChildAt(root.getChildCount()-1));
    }
}
