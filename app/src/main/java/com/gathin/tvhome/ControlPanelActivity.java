package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native side sheet. The launcher remains visible behind its translucent window. */
abstract class ControlPanelActivity extends Activity {
    final ExecutorService network = Executors.newSingleThreadExecutor();
    final Handler main = new Handler(Looper.getMainLooper());
    LinearLayout content;
    TextView status;
    TvPalette palette;
    boolean visible;
    int generation;
    private ScrollView panel;
    private ScrollView scroll;
    private FrameLayout root;
    private boolean closing;
    private boolean rebuilding;
    private FocusNavigator navigation;
    private PageMotion pageMotion;
    private ThemeWatcher themeWatcher;
    protected int panelWidth() { return 360; }
    protected abstract void buildContent();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        overridePendingTransition(0,0);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        palette = TvPalette.of(this);
        root = new FrameLayout(this);
        root.setOnClickListener(v -> finish());
        root.setFocusable(false);
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        scroll = new FocusScrollView(this); scroll.setFillViewport(true);
        pageMotion=new PageMotion(scroll);
        scroll.setVerticalScrollBarEnabled(false);
        boolean sheet = panelWidth() > 0;
        if (sheet) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams window = getWindow().getAttributes(); window.dimAmount = .28f; getWindow().setAttributes(window);
        } else root.setBackgroundColor(palette.background);
        scroll.setBackground(sheet ? GlassMaterial.surface(scroll,palette,16,true) : TvUi.shape(this,palette.background,0,0));
        if (sheet) scroll.setElevation(dp(10));
        scroll.setClipToOutline(sheet);
        scroll.setFocusable(false);
        FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(sheet ? dp(Math.min(panelWidth(), screenWidth() - 24)) : -1, -1, Gravity.END);
        if (sheet) bounds.setMargins(dp(12),dp(12),dp(12),dp(12));
        root.addView(scroll,bounds);
        content = TvUi.column(this); content.setPadding(dp(22),dp(25),dp(22),dp(28)); scroll.addView(content);
        setContentView(root); panel=sheet ? scroll : null;
        navigation=new FocusNavigator(this,this::navigationRows);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldView,newView) -> {
            if (!rebuilding && newView!=null && LivingHome.within(newView,content)) pageMotion.reveal(newView,content,dp(14),dp(22));
        });
        FrostedBackdrop.prepare(this);
        buildContent();
        themeWatcher=new ThemeWatcher(this,this::refreshContent);
        enter(scroll,sheet);
    }
    /** Rebuild from retained models, without Activity recreation or another sheet transition. */
    protected final void refreshContent() {
        if (closing || isFinishing() || isDestroyed()) return;
        FocusBookmark focus=FocusBookmark.capture(getCurrentFocus(),content);
        int y=scroll.getScrollY();
        rebuilding=true; pageMotion.stop(); scroll.animate().cancel();
        scroll.setAlpha(1); scroll.setTranslationX(0); scroll.setScaleX(1); scroll.setScaleY(1);
        boolean previousLight=palette.light; palette=TvPalette.of(this);
        if (previousLight!=palette.light) FrostedBackdrop.prepare(this);
        scroll.setBackground(panel!=null ? GlassMaterial.surface(scroll,palette,16,true) : TvUi.shape(this,palette.background,0,0));
        if (panel==null) root.setBackgroundColor(palette.background);
        content.removeAllViews(); buildContent(); navigation.wire();
        focus.restore(content);
        scroll.post(() -> { pageMotion.jumpTo(y); rebuilding=false; });
    }
    @Override protected void onResume() { super.onResume(); themeWatcher.refresh(); }
    private void enter(View view,boolean sheet) {
        if (!sheet) { TvUi.appear(view); return; }
        if (!TvMotion.enabled(this)) return;
        view.setPivotX(dp(Math.min(panelWidth(),screenWidth()-24))); view.setPivotY(dp(48));
        view.setAlpha(0); view.setTranslationX(dp(28)); view.setScaleX(.985f); view.setScaleY(.985f);
        view.animate().alpha(1).translationX(0).scaleX(1).scaleY(1).setDuration(260)
                .setInterpolator(TvUi.EASE_DRAWER).withLayer().start();
    }
    private java.util.List<FocusNavigator.Row> navigationRows() {
        java.util.List<FocusNavigator.Row> rows=new java.util.ArrayList<>();
        panelRows(rows,content,"panel"); return rows;
    }
    private void panelRows(java.util.List<FocusNavigator.Row> rows,View view,String path) {
        if (view instanceof LinearLayout && !view.isFocusable()
                && ((LinearLayout)view).getOrientation()==LinearLayout.VERTICAL) {
            LinearLayout group=(LinearLayout)view;
            for (int i=0;i<group.getChildCount();i++) panelRows(rows,group.getChildAt(i),path+"-"+i);
        } else FocusNavigator.add(rows,path,view);
    }
    @Override protected void onPostCreate(Bundle state) { super.onPostCreate(state); navigation.wire(); }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        return navigation!=null && navigation.key(event) || super.dispatchKeyEvent(event);
    }
    private int screenWidth() { return Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density); }
    void header(int title) { header(getString(title)); }
    void header(String title) {
        LinearLayout row = row();
        TextView heading=TvUi.title(this,title,24,palette.text); TvAccessibility.heading(heading);
        row.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        View close = TvUi.iconButton(this,"close",getString(R.string.hc_back),true,this::finish);
        row.addView(close,TvUi.params(this,48,48,0)); if (!rebuilding) close.requestFocus();
        status = TvUi.text(this,"",16,palette.muted); TvAccessibility.status(status); content.addView(status); TvUi.gap(content,14);
    }
    LinearLayout row() {
        LinearLayout row = TvUi.row(this); row.setPadding(0,dp(3),0,dp(3));
        content.addView(row,new LinearLayout.LayoutParams(-1,-2)); return row;
    }
    TextView text(String value,int size) { return TvUi.text(this,value,size,palette.text); }
    TextView button(LinearLayout row,String label,Runnable action) {
        TextView view = TvUi.plain(this,label,action); row.addView(view,TvUi.params(this,-2,-2,8)); return view;
    }
    void section(String label) {
        TvUi.gap(content,18); TextView heading=TvUi.title(this,label,18,palette.text); TvAccessibility.heading(heading);
        content.addView(heading); TvUi.gap(content,8);
    }
    void item(String label,String detail,Runnable action) {
        LinearLayout line = TvUi.column(this); line.setPadding(dp(14),dp(14),dp(14),dp(14));
        line.addView(TvUi.title(this,label,16,palette.text));
        if (!detail.isEmpty()) { TvUi.gap(line,4); line.addView(TvUi.text(this,detail,14,palette.muted)); }
        line.setMinimumHeight(dp(56)); line.setContentDescription(detail.isEmpty() ? label : label+". "+detail);
        TvUi.focus(line,palette,12,1,0,null); line.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-1,-2); bounds.setMargins(0,dp(2),0,dp(6)); content.addView(line,bounds);
    }
    int dp(float value) { return TvUi.dp(this,value); }
    @Override public void finish() {
        if (panel==null || !TvMotion.enabled(this)) { finishNow(); return; }
        if (closing) return;
        closing=true; visible=false; generation++; main.removeCallbacksAndMessages(null);
        panel.animate().cancel(); panel.animate().alpha(0).translationX(dp(28)).scaleX(.985f).scaleY(.985f).setDuration(180)
                .setInterpolator(TvUi.EASE_OUT).withLayer()
                .withEndAction(this::finishNow).start();
    }
    private void finishNow() { super.finish(); overridePendingTransition(0,0); }
    @Override protected void onStart() { super.onStart(); visible = true; }
    @Override protected void onStop() { visible = false; generation++; main.removeCallbacksAndMessages(null); pageMotion.stop(); super.onStop(); }
    @Override protected void onDestroy() { if (themeWatcher!=null) themeWatcher.close(); network.shutdownNow(); super.onDestroy(); }
}
