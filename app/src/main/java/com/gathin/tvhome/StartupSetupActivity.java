package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;

/** Explains and opens the platform permission; never grants accessibility or vendor exceptions. */
public final class StartupSetupActivity extends ControlPanelActivity {
    @Override protected void buildContent() {
        header(TvUi.label(this,"Start on power on","Iniciar al encender"));
        boolean enabled=StartupState.enabled(this),access=access();
        status.setText(TvUi.label(this,access ? "Android access enabled" : "Android access needed",access ? "Acceso Android habilitado" : "Falta acceso de Android"));
        content.addView(TvUi.text(this,TvUi.label(this,"Opens Home once when Android starts. On wake, it can replace the original system Home during a short window. The service does not read screen content or intercept remote buttons.","Abre Home una vez al iniciar Android. Al despertar, puede reemplazar el Home original durante una ventana breve. El servicio no lee la pantalla ni intercepta botones del control."),14,palette.muted));
        section(TvUi.label(this,"Startup","Inicio"));
        item(TvUi.label(this,"Start on power on","Iniciar al encender"),TvUi.label(this,enabled ? "On" : "Off",enabled ? "Activado" : "Desactivado"),() -> {
            if (!StartupState.configure(this,!StartupState.enabled(this))) android.widget.Toast.makeText(this,R.string.hc_unavailable,android.widget.Toast.LENGTH_LONG).show();
            refreshContent();
        });
        item(TvUi.label(this,"Android accessibility settings","Accesibilidad de Android"),"Soft Glass Launcher · "+TvUi.label(this,"Start on power on","Iniciar al encender"),() -> SystemLinks.open(this,new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        TvUi.gap(content,12); content.addView(TvUi.text(this,TvUi.label(this,"Some TVs also require allowing background startup in their own app settings. No such permission is changed automatically.","Algunas TVs también requieren permitir inicio en segundo plano desde sus ajustes de apps. Ese permiso no se cambia automáticamente."),14,palette.muted));
    }
    private boolean access() {
        String enabled=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled==null) return false;
        ComponentName own=new ComponentName(this,StartupHomeService.class);
        for (String value:enabled.split(":")) if (own.equals(ComponentName.unflattenFromString(value))) return true;
        return false;
    }
    @Override protected void onResume() { super.onResume(); refreshContent(); }
}
