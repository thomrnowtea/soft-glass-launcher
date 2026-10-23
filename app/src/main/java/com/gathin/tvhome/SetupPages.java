package com.gathin.tvhome;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small page builders share the launcher materials, system IME and real installed-app catalogue. */
final class SetupPages {
    static void build(SetupActivity page,int step) {
        switch (step) {
            case 0: welcome(page); break;
            case 1: appearance(page); break;
            case 2: apps(page); break;
            case 3: weather(page); break;
            case 4: connections(page); break;
            default: ready(page);
        }
    }
    private static void welcome(SetupActivity page) {
        preview(page);
        page.note(page.label("Choose your apps and optional connections. Everything can be changed later in Settings.","Elegí tus apps y las conexiones que quieras usar. Todo se puede cambiar después en Settings."));
    }
    private static void appearance(SetupActivity page) {
        TvPreferences preferences=new TvPreferences(page);
        LinearLayout row=TvUi.row(page); page.body.addView(row); TvUi.gap(page.body,16);
        String[] keys={"dark","light","system"},en={"Dark","Light","System"},es={"Oscuro","Claro","Sistema"};
        for (int i=0;i<keys.length;i++) {
            String key=keys[i];
            TextView item=page.button(page.label(en[i],es[i]),() -> preferences.text("theme",key));
            boolean selected=preferences.theme().equals(key);
            if (selected) item.setBackground(GlassMaterial.emphasis(page,page.palette,14,true));
            if (selected) item.setTextColor(page.palette.light ? 0xFFFFFFFF : page.palette.onAccent);
            TvAccessibility.power(item,item.getText().toString(),page.label(selected ? "Selected" : "Not selected",selected ? "Seleccionado" : "Sin seleccionar"),page.label("Select theme","Elegir tema"),selected,true,false);
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(0,page.dp(48),1); size.setMarginEnd(page.dp(i==2 ? 0 : 10)); row.addView(item,size);
        }
        preview(page);
        page.option(page.label("Reduced motion","Movimiento reducido"),page.label(preferences.reduced() ? "On · gentler transitions" : "Off · fluid transitions",preferences.reduced() ? "Activado · transiciones suaves" : "Desactivado · transiciones fluidas"),
                () -> { preferences.flag("reduced",!preferences.reduced()); page.refreshChoices(); });
    }
    private static void preview(SetupActivity page) {
        LinearLayout preview=TvUi.column(page); preview.setPadding(page.dp(22),page.dp(20),page.dp(22),page.dp(20));
        preview.setBackground(GlassMaterial.card(page,page.palette,22));
        preview.addView(TvUi.text(page,java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),32,page.palette.text));
        TvUi.gap(preview,16); preview.addView(TvUi.title(page,page.label("Your selected apps","Tus apps seleccionadas"),17,page.palette.text)); TvUi.gap(preview,12);
        LinearLayout row=TvUi.row(page); preview.addView(row);
        int count=0;
        for (String pkg:page.selected) {
            AppAdapter.Entry app=find(page,pkg); if (app==null) continue;
            ImageView image=new ImageView(page); image.setImageDrawable(app.banner==null ? app.icon : app.banner); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            image.setBackground(TvUi.shape(page,page.palette.surfaceAlt,0,8)); image.setClipToOutline(true); image.setContentDescription(app.label);
            LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(0,page.dp(48),1); bounds.setMarginEnd(page.dp(7)); row.addView(image,bounds);
            if (++count==6) break;
        }
        if (count==0) row.addView(TvUi.text(page,page.label("Choose apps in the next step","Elegí apps en el siguiente paso"),14,page.palette.muted));
        page.body.addView(preview,TvUi.params(page,-1,-2,0)); TvUi.gap(page.body,18);
    }
    private static AppAdapter.Entry find(SetupActivity page,String pkg) { for (AppAdapter.Entry entry:page.apps) if (entry.pkg.equals(pkg)) return entry; return null; }
    private static void apps(SetupActivity page) {
        if (page.apps.isEmpty()) { page.note(page.label("No launchable apps found. You can add apps later from your TV's store.","No hay apps disponibles. Podés agregarlas después desde la tienda de la TV.")); return; }
        LinearLayout row=null;
        for (int i=0;i<page.apps.size();i++) {
            if (i%2==0) { row=TvUi.row(page); row.setBaselineAligned(false); page.body.addView(row); TvUi.gap(page.body,9); }
            app(page,row,page.apps.get(i));
        }
        page.note(page.label("Hold an app on Home to reorder it. All apps keeps the complete catalogue.","Mantené una app en Home para ordenarla. All apps conserva el catálogo completo."));
    }
    private static void app(SetupActivity page,LinearLayout row,AppAdapter.Entry app) {
        LinearLayout tile=TvUi.row(page); tile.setPadding(page.dp(12),page.dp(10),page.dp(12),page.dp(10)); tile.setMinimumHeight(page.dp(60));
        ImageView icon=new ImageView(page); icon.setImageDrawable(app.icon); icon.setScaleType(ImageView.ScaleType.FIT_CENTER); TvAccessibility.decorative(icon); tile.addView(icon,TvUi.params(page,30,30,10));
        TextView name=TvUi.title(page,app.label,14,page.palette.text); name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END); tile.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        TextView mark=TvUi.text(page,"",16,page.palette.text); mark.setGravity(Gravity.CENTER); tile.addView(mark,TvUi.params(page,24,28,0));
        TvUi.focus(tile,page.palette,14,1,0,null);
        paintApp(page,app,tile,mark);
        tile.setOnClickListener(view -> {
            if (!page.selected.remove(app.pkg)) page.selected.add(app.pkg);
            SetupState.saveSelection(page,page.selected); paintApp(page,app,tile,mark); page.updateCount();
        });
        LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(0,-1,1); bounds.setMarginEnd(page.dp(9)); row.addView(tile,bounds);
    }
    private static void paintApp(SetupActivity page,AppAdapter.Entry app,View tile,TextView mark) {
        boolean selected=page.selected.contains(app.pkg); mark.setText(selected ? "✓" : "○");
        tile.setBackground(selected ? GlassMaterial.card(page,page.palette,14) : GlassMaterial.surface(page,page.palette,14,false));
        TvAccessibility.power(tile,app.label,page.label(selected ? "Selected" : "Not selected",selected ? "Seleccionada" : "Sin seleccionar"),page.label(selected ? "Remove from Home" : "Add to Home",selected ? "Quitar de Home" : "Agregar a Home"),selected,true,false);
    }
    private static void weather(SetupActivity page) {
        WeatherLocation location=WeatherLocation.load(page);
        page.option(page.label("Search city","Buscar ciudad"),WeatherLocation.configured(page) ? location.name+" · "+location.detail : page.label("City, country","Ciudad, país"),() -> page.open(WeatherLocationActivity.class));
        page.note(page.label("No exact address or location permission needed. If you skip this, weather stays off until you choose a city.","No necesita tu dirección exacta ni permiso de ubicación. Si omitís este paso, el clima queda desactivado hasta elegir una ciudad."));
    }
    private static void connections(SetupActivity page) {
        boolean linked=new StremioStore(page).linked();
        page.option("Stremio",page.label(linked ? "Linked · Continue Watching" : "Continue Watching on Home",linked ? "Vinculado · Seguir viendo" : "Seguir viendo en Home"),() -> integration(page,"com.stremio.one",StremioLinkActivity.class));
        page.option("Spotify",SpotifyAccess.summary(page),() -> integration(page,"com.spotify.tv.android",SpotifyAccessActivity.class));
        page.option(page.label("WiZ lights","Luces WiZ"),page.label("Discover lights on this network · on/off","Buscar luces en esta red · encendido/apagado"),() -> page.open(LightsActivity.class));
        page.option(page.label("Wi-Fi name","Nombre Wi-Fi"),page.label("Android requires location access to show the network name.","Android requiere ubicación para mostrar el nombre de la red."),() -> NetworkNameAccess.request(page));
        page.option("TMDB",page.label(new TmdbAuth(page).configured() ? "Artwork key configured" : "Optional artwork key · your own credential",new TmdbAuth(page).configured() ? "Clave de imágenes configurada" : "Clave opcional para imágenes · tu propia credencial"),() -> credential(page));
    }
    private static void integration(SetupActivity page,String pkg,Class<?> settings) {
        if (SystemCapabilities.installed(page,pkg)) { page.open(settings); return; }
        Intent market=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+pkg)).setPackage("com.android.vending");
        if (SystemCapabilities.resolves(page,market)) page.external(market);
        else android.widget.Toast.makeText(page,page.label("Install this app from your TV's store first.","Primero instalá esta app desde la tienda de la TV."),android.widget.Toast.LENGTH_LONG).show();
    }
    private static void credential(SetupActivity page) {
        TvTextDialog.show(page,"TMDB","","API key / Read Access Token",true,4096,value -> {
            if (!new TmdbAuth(page).save(value)) return page.label("Check the key format","Revisá el formato de la clave");
            page.refreshChoices(); return null;
        });
    }
    private static void ready(SetupActivity page) {
        page.note(page.label("Apps, appearance and optional connections are ready. You can reopen this guide in Settings → Customize → Setup guide.","Apps, apariencia y conexiones opcionales listas. Podés volver a abrir la guía en Settings → Customize → Setup guide."));
        String home=SystemCapabilities.defaultHome(page) ? page.label("Already the default Home","Ya es el Home predeterminado")
                : page.label("Opens the Android Home selector, when available.","Abre el selector Home de Android, si está disponible.");
        page.option(page.label("Choose default Home","Elegir Home predeterminado"),home,() -> defaultHome(page));
        page.option(page.label("Start on power on","Iniciar al encender"),page.label("Optional · uses Android accessibility permission","Opcional · utiliza el permiso de accesibilidad de Android"),() -> page.open(StartupSetupActivity.class));
        page.note(page.label("Some TVs restrict the Home button or background startup. The launcher keeps your TV's original Home available.","Algunas TVs restringen el botón Home o el inicio en segundo plano. El Home original de tu TV sigue disponible."));
    }
    private static void defaultHome(SetupActivity page) {
        if (SystemCapabilities.homeSettings(page)) { page.external(new Intent(Settings.ACTION_HOME_SETTINGS)); return; }
        TvUi.dialog(page).setTitle(page.label("Default Home","Home predeterminado"))
                .setMessage(page.label("This TV does not expose a Home selector. Check Settings → Apps → Default apps. If unavailable, use your remote-button mapper to open Soft Glass Launcher.","Esta TV no ofrece el selector Home. Revisá Settings → Apps → Default apps. Si no está disponible, usá tu app de mapeo del control para abrir Soft Glass Launcher."))
                .setPositiveButton(page.label("Open TV settings","Abrir ajustes TV"),(dialog,which) -> SystemLinks.open(page,new Intent(Settings.ACTION_SETTINGS))).setNegativeButton(page.label("Close","Cerrar"),null).show();
    }
}
