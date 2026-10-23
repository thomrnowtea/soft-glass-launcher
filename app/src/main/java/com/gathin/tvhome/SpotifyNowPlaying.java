package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Callback-only observation while Home is visible. No playback controls, polling or account access. */
final class SpotifyNowPlaying {
    private final Context context;
    private final MediaSessionManager manager;
    private final Consumer<MediaMetadata> changed;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<MediaController> controllers=new ArrayList<>();
    private boolean active,registered;
    private final MediaController.Callback callback=new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) { render(); }
        @Override public void onMetadataChanged(MediaMetadata data) { render(); }
        @Override public void onSessionDestroyed() { refresh(); }
    };
    private final MediaSessionManager.OnActiveSessionsChangedListener listener=this::sessions;
    SpotifyNowPlaying(Context context,Consumer<MediaMetadata> changed) {
        this.context=context; this.changed=changed;
        manager=(MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    }
    void start() {
        if (active) return;
        active=true; NowPlayingAccess.connectionChanged=this::refresh; refresh();
    }
    private void refresh() {
        if (!active || manager==null) return;
        if (!SpotifyAccess.granted(context)) { clearAccess(); return; }
        ComponentName access=new ComponentName(context,NowPlayingAccess.class);
        try {
            if (!registered) { manager.addOnActiveSessionsChangedListener(listener,access,main); registered=true; }
            sessions(manager.getActiveSessions(access));
        } catch (SecurityException error) { clearAccess(); }
    }
    private void clearAccess() {
        detach();
        if (registered && manager!=null) manager.removeOnActiveSessionsChangedListener(listener);
        registered=false; changed.accept(null);
    }
    private void sessions(List<MediaController> sessions) {
        detach(); if (!active) return;
        if (sessions!=null) for (MediaController controller:sessions) {
            if (!controller.getPackageName().startsWith("com.spotify.")) continue;
            controllers.add(controller); controller.registerCallback(callback,main);
        }
        render();
    }
    private void render() {
        if (!active) return;
        for (MediaController controller:controllers) {
            PlaybackState state=controller.getPlaybackState(); MediaMetadata data=controller.getMetadata();
            if (state==null || state.getState()!=PlaybackState.STATE_PLAYING || data==null) continue;
            CharSequence title=data.getText(MediaMetadata.METADATA_KEY_TITLE);
            if (title!=null && title.length()>0) { changed.accept(data); return; }
        }
        changed.accept(null);
    }
    private void detach() { for (MediaController controller:controllers) controller.unregisterCallback(callback); controllers.clear(); }
    void stop() {
        active=false; NowPlayingAccess.connectionChanged=null; detach();
        if (registered && manager!=null) manager.removeOnActiveSessionsChangedListener(listener);
        registered=false; changed.accept(null);
    }
}
