package com.gathin.tvhome;

import android.content.Intent;

/** Explicit playback-access status instead of sending this TV to a missing settings screen. */
public final class SpotifyAccessActivity extends ControlPanelActivity {
    private boolean access;
    @Override protected void buildContent() {
        header(TvUi.label(this,"Spotify mini player","Mini reproductor de Spotify"));
        access=SpotifyAccess.granted(this); status.setText(SpotifyAccess.summary(this));
        content.addView(text(TvUi.label(this,"Shows the current track only while Spotify is playing. Select it to open Spotify.",
                "Muestra la canción actual solo mientras Spotify está reproduciendo. Seleccionala para abrir Spotify."),16));
        TvUi.gap(content,18);
        Intent settings=SpotifyAccess.settings(this);
        if (settings!=null) item(TvUi.label(this,"Notification access","Acceso a notificaciones"),
                TvUi.label(this,"Enable access for ","Habilitá el acceso para ")+getString(R.string.app_name),() -> SystemLinks.open(this,settings));
        else if (!access) content.addView(TvUi.text(this,TvUi.label(this,
                "This TV does not provide a Notification access screen. Playback access needs to be enabled separately.",
                "Esta TV no ofrece la pantalla de acceso a notificaciones. El acceso a reproducción se debe habilitar por separado."),16,palette.muted));
        TvUi.gap(content,18);
        content.addView(TvUi.text(this,TvUi.label(this,"No notifications are saved or sent. Only Spotify playback is used.",
                "No se guardan ni envían notificaciones. Solo se utiliza la reproducción de Spotify."),14,palette.muted));
    }
    @Override protected void onResume() {
        super.onResume();
        if (access!=SpotifyAccess.granted(this)) refreshContent();
    }
}
