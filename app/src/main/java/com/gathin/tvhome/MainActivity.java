package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;

/** Stable Home component. Presentation, catalogues and network lifecycles live in their own sections. */
public class MainActivity extends Activity {
    static final String ACTION_LAUNCHER_SETTINGS = BuildConfig.APPLICATION_ID+".action.LAUNCHER_SETTINGS";
    static final String ACTION_MOVE_APP=BuildConfig.APPLICATION_ID+".action.MOVE_APP";
    static int COLUMNS = 4;
    private LivingHome screen;
    private ThemeWatcher themeWatcher;
    private boolean started,resumed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SetupState.initialize(this);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        build();
        themeWatcher=new ThemeWatcher(this,() -> { build(); if (resumed) screen.resume(); });
    }
    private void build() {
        LivingHome.ViewState state=screen==null ? null : screen.saveViewState();
        if (screen != null) screen.close();
        screen = new LivingHome(this); setContentView(screen.root);
        if (state!=null) screen.restoreViewState(state);
        if (started) screen.start();
    }
    @Override protected void onStart() { super.onStart(); started=true; screen.start(); }
    @Override protected void onResume() {
        super.onResume();
        StartupState.recordResume(this,getIntent());
        if (SetupState.needed(this)) {
            startActivity(new Intent(this,SetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            return;
        }
        resumed=true; themeWatcher.refresh();
        if (!screen.styleSignature.equals(LivingHome.style(this))) build();
        screen.resume();
        pendingMove();
        if (ACTION_LAUNCHER_SETTINGS.equals(getIntent().getAction())) {
            setIntent(new Intent(Intent.ACTION_MAIN)); screen.settings();
        }
    }
    @Override protected void onPause() { resumed=false; screen.pause(); super.onPause(); }
    @Override protected void onStop() { started=false; screen.stop(); super.onStop(); }
    @Override protected void onDestroy() { if (themeWatcher!=null) themeWatcher.close(); screen.close(); super.onDestroy(); }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (ACTION_LAUNCHER_SETTINGS.equals(intent.getAction())) {
            setIntent(new Intent(Intent.ACTION_MAIN)); screen.settings();
        } else if (ACTION_MOVE_APP.equals(intent.getAction())) pendingMove();
        else screen.home();
    }
    private void pendingMove() {
        if (!ACTION_MOVE_APP.equals(getIntent().getAction())) return;
        String pkg=getIntent().getStringExtra("package"); setIntent(new Intent(Intent.ACTION_MAIN));
        if (pkg!=null) screen.moveApp(pkg);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if (request==SearchControl.VOICE_REQUEST && screen!=null) screen.voiceResult(result,data);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if (request==NetworkNameAccess.REQUEST && screen!=null) screen.refreshConnections();
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        return screen != null && screen.key(event) || super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() { screen.home(); }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (themeWatcher!=null) themeWatcher.refresh();
    }
}
