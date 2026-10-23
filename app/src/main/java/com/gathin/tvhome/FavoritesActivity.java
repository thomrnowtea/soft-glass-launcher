package com.gathin.tvhome;

import android.content.Intent;
import android.widget.LinearLayout;
import java.util.List;

/** Removing from Home only unpins an app. All apps always keeps the full launchable catalogue. */
public final class FavoritesActivity extends ControlPanelActivity {
    private TvPreferences preferences;
    private List<String> pinned;
    private List<AppAdapter.Entry> apps;
    private LinearLayout directory;
    @Override protected void buildContent() { header(TvUi.label(this,"Manage apps","Organizar apps"));
        preferences=new TvPreferences(this);
        AppCatalog catalog=new AppCatalog(this); apps=catalog.reload(); catalog.favorites(); pinned=preferences.favorites(Config.load(this).dockApps); status.setText(TvUi.label(this,"Select an app to move, add or remove it from Home.","Elegí una app para moverla, agregarla o quitarla de Inicio."));
        directory=TvUi.column(this); content.addView(directory); render();
    }
    private void render() {
        directory.removeAllViews();
        for (AppAdapter.Entry app:apps) {
            String subtitle=TvUi.label(this,pinned.contains(app.pkg) ? "On Home" : "All apps only",pinned.contains(app.pkg) ? "En Inicio" : "Solo en Todas las apps");
            android.widget.TextView row=TvUi.plain(this,app.label+"\n"+subtitle,() -> options(app));
            directory.addView(row,new LinearLayout.LayoutParams(-1,-2)); TvUi.line(directory);
        }
    }
    private void options(AppAdapter.Entry app) {
        boolean home=pinned.contains(app.pkg);
        String[] actions=home ? new String[]{TvUi.label(this,"Move","Mover"),TvUi.label(this,"Remove from Home","Quitar de Inicio"),TvUi.label(this,"Done","Listo")}
                : new String[]{TvUi.label(this,"Add to Home","Agregar a Inicio"),TvUi.label(this,"Done","Listo")};
        TvUi.dialog(this).setTitle(app.label).setItems(actions,(dialog,index) -> {
            if (home && index==0) {
                startActivity(new Intent(this,MainActivity.class).setAction(MainActivity.ACTION_MOVE_APP).putExtra("package",app.pkg)); finish(); return;
            }
            if (home && index==1) { preferences.hideHomeApp(app.pkg,Config.load(this).dockApps); pinned.remove(app.pkg); save(app.pkg); }
            if (!home && index==0) { preferences.showHomeApp(app.pkg,Config.load(this).dockApps); pinned.add(app.pkg); save(app.pkg); }
        }).show();
    }
    private void save(String pkg) {
        preferences.saveFavorites(pinned); render();
        for (int i=0;i<apps.size();i++) if (apps.get(i).pkg.equals(pkg)) { directory.getChildAt(i*2).requestFocus(); return; }
    }
}
