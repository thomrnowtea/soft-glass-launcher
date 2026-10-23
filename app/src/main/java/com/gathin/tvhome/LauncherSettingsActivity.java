package com.gathin.tvhome;

import android.content.Intent;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class LauncherSettingsActivity extends ControlPanelActivity {
    private TvPreferences preferences;
    private String placeSignature;
    private boolean spotifyAccess;
    @Override protected void buildContent() {
        header(TvUi.label(this,"Customize","Personalizar"));
        preferences=new TvPreferences(this); status.setVisibility(android.view.View.GONE);
        appearance(); homeOptions(); widgetOptions(); motionAndVoice(); connections(); attributions();
    }
    private void appearance() {
        section(TvUi.label(this,"Appearance","Apariencia"));
        LinearLayout themes=TvUi.row(this); content.addView(themes);
        theme(themes,"dark","Dark","Oscuro"); theme(themes,"light","Light","Claro"); theme(themes,"system","System","Sistema");
    }
    private void homeOptions() {
        section(TvUi.label(this,"Home","Inicio"));
        item(TvUi.label(this,"Setup guide","Guía de configuración"),"Soft Glass Launcher",() -> startActivity(new Intent(this,SetupActivity.class).putExtra(SetupActivity.REVISIT,true)));
        item(TvUi.label(this,"Start on power on","Iniciar al encender"),on(StartupState.enabled(this)),() -> open(StartupSetupActivity.class));
        TextView hint=TvUi.text(this,TvUi.label(this,"Hold an app on the Home slider to move it. Press OK to finish.","Mantené una app del slider para moverla. Presioná OK para terminar."),14,palette.muted);
        hint.setPadding(dp(14),dp(8),dp(14),dp(10)); content.addView(hint);
        WeatherLocation place=WeatherLocation.load(this); placeSignature=place.signature();
        item(TvUi.label(this,"Weather location","Ciudad del pronóstico"),place.name+" · "+place.detail,() -> open(WeatherLocationActivity.class));
    }
    private void widgetOptions() {
        section("Widgets");
        for (HomeWidget widget:HomeWidget.values()) {
            boolean shown=preferences.widget(widget);
            item(widget.label(this),on(shown),() -> preferences.widget(widget,!preferences.widget(widget)));
            TvAccessibility.power(content.getChildAt(content.getChildCount()-1),widget.label(this),on(shown),
                    TvUi.label(this,shown ? "Hide widget" : "Show widget",shown ? "Ocultar widget" : "Mostrar widget"),shown,true,false);
        }
    }
    private void motionAndVoice() {
        section(TvUi.label(this,"Motion and voice","Movimiento y voz"));
        item(TvUi.label(this,"Animations","Animaciones"),on(preferences.motion()),() -> { preferences.flag("motion",!preferences.motion()); recreate(); });
        item(TvUi.label(this,"Reduced motion","Movimiento reducido"),on(preferences.reduced()),() -> { preferences.flag("reduced",!preferences.reduced()); recreate(); });
        item(TvUi.label(this,"Voice language","Idioma de voz"),java.util.Locale.forLanguageTag(preferences.voiceLanguage()).getDisplayName(),this::voiceLanguage);
        item(TvUi.label(this,"System accessibility","Accesibilidad del sistema"),"",() -> SystemLinks.open(this,new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
    }
    private void connections() {
        section(TvUi.label(this,"Connections","Conexiones")); spotifyAccess=SpotifyAccess.granted(this);
        boolean linked=new StremioStore(this).linked();
        item("Stremio",TvUi.label(this,linked ? "Linked · Continue Watching" : "Link Continue Watching",linked ? "Vinculado · Seguir viendo" : "Vincular Seguir viendo"),() -> open(StremioLinkActivity.class));
        item(TvUi.label(this,"Spotify mini player","Mini reproductor de Spotify"),SpotifyAccess.summary(this),() -> open(SpotifyAccessActivity.class));
        item(TvUi.label(this,"Wi-Fi network name","Nombre de la red Wi-Fi"),TvUi.label(this,"Android requires location permission to show the network name.","Android requiere permiso de ubicación para mostrar el nombre de la red."),this::wifiPermission);
        boolean configured=new TmdbAuth(this).configured();
        item("TMDB",TvUi.label(this,configured ? "Artwork · Configured" : "Artwork · Add key",configured ? "Imágenes · Configurado" : "Imágenes · Agregar clave"),this::credential);
    }
    private void attributions() {
        item(TvUi.label(this,"About and licenses","Acerca de y licencias"),"Soft Glass Launcher · "+BuildConfig.VERSION_NAME,this::licenses);
        TvUi.gap(content,18);
        content.addView(TvUi.text(this,"TMDB · This product uses the TMDB API but is not endorsed or certified by TMDB.",12,palette.muted));
        content.addView(TvUi.text(this,"Weather: Open-Meteo.com · CC BY 4.0",12,palette.muted));
    }
    private void licenses() {
        String credit="Soft Glass Launcher\n\nBased on CleanYourTV by Gathin (MIT).\nhttps://github.com/Gathin23/cleanyourtv\n\nManrope: SIL Open Font License 1.1.\n\n";
        try (java.io.InputStream input=getAssets().open("licenses/CleanYourTV-MIT.txt")) {
            credit+=new String(NetJson.read(input,32768),java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException unavailable) { credit+="License: https://github.com/Gathin23/cleanyourtv/blob/main/LICENSE"; }
        TvUi.dialog(this).setTitle(TvUi.label(this,"About and licenses","Acerca de y licencias")).setMessage(credit).setPositiveButton("OK",null).show();
    }
    @Override protected void onResume() {
        super.onResume();
        if (!WeatherLocation.load(this).signature().equals(placeSignature) || spotifyAccess!=SpotifyAccess.granted(this)) refreshContent();
    }
    private void open(Class<?> type) { startActivity(new Intent(this,type)); }
    private String on(boolean enabled) { return TvUi.label(this,enabled ? "On" : "Off",enabled ? "Activado" : "Desactivado"); }
    private void theme(LinearLayout row,String key,String en,String es) {
        TextView label=TvUi.button(this,TvUi.label(this,en,es),palette,() -> preferences.text("theme",key),12);
        label.setPadding(dp(8),0,dp(8),0);
        if (key.equals(preferences.theme())) { label.setBackground(GlassMaterial.emphasis(label,palette,12,true)); label.setTextColor(palette.light ? 0xFFFFFFFF : palette.onAccent); }
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); if (!key.equals("system")) p.setMarginEnd(dp(8)); row.addView(label,p);
    }
    private void voiceLanguage() {
        TvUi.dialog(this).setTitle(TvUi.label(this,"Voice language","Idioma de voz"))
                .setSingleChoiceItems(new String[]{TvUi.label(this,"System language","Idioma del sistema"),"Español","English"},-1,(dialog,index) -> {
                    preferences.text("voice_language",index==0 ? java.util.Locale.getDefault().toLanguageTag() : index==1 ? "es" : "en"); dialog.dismiss(); refreshContent();
                }).show();
    }
    private void wifiPermission() { NetworkNameAccess.request(this); }
    private void credential() {
        TvTextDialog.show(this,"TMDB","","API key / Read Access Token",true,4096,key -> {
            if (!new TmdbAuth(this).save(key)) return TvUi.label(this,"Check the key format","Revisá el formato de la clave"); recreate(); return null;
        });
    }
}
