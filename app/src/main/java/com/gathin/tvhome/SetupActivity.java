package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Resumable TV setup. Optional integrations open their real setup screens only on user selection. */
public final class SetupActivity extends Activity {
    static final String REVISIT="revisit";
    private static final String[] EN={"Welcome","Appearance","Your apps","Weather","Connections","Ready"};
    private static final String[] ES={"Bienvenida","Apariencia","Tus apps","Clima","Conexiones","Listo"};
    private static final String[] TITLES_EN={"Set up your Home","Choose the appearance","Choose your apps","Choose your forecast city","Connect what you use","Your Home is ready"};
    private static final String[] TITLES_ES={"Configurá tu Home","Elegí la apariencia","Elegí tus apps","Elegí la ciudad del pronóstico","Conectá lo que usás","Tu Home está listo"};
    private static final String[] DESCRIPTIONS_EN={"A few choices to get started. Your accounts and preferences stay on this TV.","Light, dark or follow your TV. Changes appear immediately.","Selected apps appear in your slider. All apps keeps the complete catalogue.","Search by city and country. You can leave weather off.","Every connection is optional. You can configure them later.","Choose how to open Home. System support varies between TVs."};
    private static final String[] DESCRIPTIONS_ES={"Unas elecciones para empezar. Tus cuentas y preferencias quedan en esta TV.","Claro, oscuro o el tema de la TV. Los cambios se ven al instante.","Las seleccionadas aparecen en tu slider. All apps conserva el catálogo completo.","Buscá por ciudad y país. Podés dejar el clima desactivado.","Todas las conexiones son opcionales. Podés configurarlas después.","Elegí cómo abrir Home. Las opciones dependen de cada TV."};
    final List<AppAdapter.Entry> apps=new ArrayList<>();
    List<String> selected;
    TvPalette palette;
    LinearLayout body;
    private LinearLayout root,sidebar,footer;
    private ScrollView scroll;
    private TextView count;
    private FocusNavigator navigation;
    private PageMotion motion;
    private ThemeWatcher themes;
    private int step;
    private boolean child;
    private boolean rebuilding;
    private boolean pendingLights;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); getWindow().getDecorView().setSystemUiVisibility(5894);
        SetupState.initialize(this);
        if (saved==null && getIntent().getBooleanExtra(REVISIT,false)) SetupState.revisit(this);
        step=SetupState.step(this);
        for (AppAdapter.Entry app:new AppCatalog(this).reload()) if (!app.pkg.equals(getPackageName())) apps.add(app);
        selected=SetupState.selected(this,apps);
        render(false);
        themes=new ThemeWatcher(this,() -> render(false));
    }
    String label(String english,String spanish) { return TvUi.label(this,english,spanish); }
    private void render(boolean animate) {
        FocusBookmark focus=root==null ? null : FocusBookmark.capture(getCurrentFocus(),root);
        if (motion!=null) motion.stop();
        rebuilding=true; palette=TvPalette.of(this);
        root=TvUi.row(this); root.setGravity(Gravity.TOP); root.setPadding(dp(32),dp(28),dp(32),dp(28));
        root.setBackground(background());
        sidebar=TvUi.column(this);
        ScrollView rail=new ScrollView(this); rail.setVerticalScrollBarEnabled(false); rail.addView(sidebar);
        root.addView(rail,TvUi.params(this,178,-1,28));
        sidebar();
        LinearLayout workspace=TvUi.column(this); root.addView(workspace,new LinearLayout.LayoutParams(0,-1,1));
        headings(workspace);
        scroll=new FocusScrollView(this); scroll.setVerticalScrollBarEnabled(false); scroll.setClipToPadding(false);
        motion=new PageMotion(scroll);
        body=TvUi.column(this); body.setPadding(dp(4),dp(20),dp(4),dp(16)); scroll.addView(body);
        workspace.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        SetupPages.build(this,step); footer(workspace);
        setContentView(root);
        FrostedBackdrop.home(root);
        navigation=new FocusNavigator(this,this::navigationRows);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldView,newView) -> {
            if (!rebuilding && LivingHome.within(newView,body)) motion.reveal(newView,body,dp(10),dp(14));
        });
        navigation.wire();
        root.post(() -> {
            rebuilding=false;
            if (focus!=null && !animate) focus.restore(root);
            if (getCurrentFocus()==null) firstChoice();
        });
        if (animate) enter();
    }
    private void sidebar() {
        LinearLayout brand=TvUi.row(this);
        android.widget.ImageView mark=new android.widget.ImageView(this); mark.setImageResource(R.drawable.sg_mark);
        mark.setImageTintList(android.content.res.ColorStateList.valueOf(palette.text)); TvAccessibility.decorative(mark);
        brand.addView(mark,TvUi.params(this,34,40,10)); brand.addView(TvUi.title(this,"Soft Glass\nLauncher",17,palette.text)); sidebar.addView(brand);
        TvUi.gap(sidebar,30);
        for (int i=0;i<EN.length;i++) {
            int index=i;
            TextView item=TvUi.button(this,(i+1)+"   "+label(EN[i],ES[i]),palette,() -> go(index),13);
            item.setTextSize(14); item.setGravity(Gravity.CENTER_VERTICAL); item.setPadding(dp(13),0,dp(10),0);
            item.setBackground(i==step ? GlassMaterial.card(this,palette,13) : null);
            if (i==step) item.setContentDescription(item.getText()+". "+label("Current step","Paso actual"));
            sidebar.addView(item,TvUi.params(this,-1,48,0)); TvUi.gap(sidebar,3);
        }
        TvUi.gap(sidebar,18); sidebar.addView(TvUi.text(this,label("Use the arrows to move.\nPress OK to choose.","Movete con las flechas.\nPresioná OK para elegir."),12,palette.muted));
    }
    private void headings(LinearLayout workspace) {
        TextView eyebrow=TvUi.text(this,label("SETUP","CONFIGURACIÓN")+" · "+(step+1)+" / 6",12,palette.muted); workspace.addView(eyebrow); TvUi.gap(workspace,8);
        TextView title=TvUi.title(this,label(TITLES_EN[step],TITLES_ES[step]),29,palette.text); TvAccessibility.heading(title); workspace.addView(title);
        TvUi.gap(workspace,7); TextView description=TvUi.text(this,label(DESCRIPTIONS_EN[step],DESCRIPTIONS_ES[step]),14,palette.muted);
        description.setLineSpacing(dp(3),1); workspace.addView(description);
    }
    private void footer(LinearLayout workspace) {
        TvUi.line(workspace); TvUi.gap(workspace,13);
        footer=TvUi.row(this); workspace.addView(footer);
        if (step>0) footer.addView(button(label("Back","Atrás"),() -> go(step-1)),TvUi.params(this,-2,48,8));
        count=TvUi.text(this,"",12,palette.muted); count.setPadding(dp(8),0,dp(8),0); footer.addView(count,new LinearLayout.LayoutParams(0,-2,1)); updateCount();
        if (step<5) footer.addView(button(laterLabel(),() -> { if (step==0) done(); else go(step+1); }),TvUi.params(this,-2,48,8));
        TextView next=button(nextLabel(),this::next);
        next.setBackground(GlassMaterial.emphasis(this,palette,14,true)); next.setTextColor(palette.light ? 0xFFFFFFFF : palette.onAccent);
        footer.addView(next,TvUi.params(this,-2,48,0));
    }
    private String laterLabel() { return step==0 ? label("Set up later","Configurar después") : label("Skip","Omitir"); }
    private String nextLabel() {
        if (step==5) return label("Open Home  →","Abrir Home  →");
        if (step==0) return label("Get started  →","Comenzar  →");
        return label("Continue  →","Continuar  →");
    }
    void updateCount() { if (count!=null) count.setText(step==2 ? label(selected.size()+" selected",selected.size()+" seleccionadas") : ""); }
    private void next() {
        if (step==5) { done(); return; }
        if (step==2) SetupState.applySelection(this,selected,apps);
        go(step+1);
    }
    private void go(int value) {
        step=value; SetupState.step(this,step); render(true); root.post(this::firstChoice);
    }
    private void firstChoice() {
        for (FocusNavigator.Row row:navigationRows()) {
            if (!row.name.equals("setup-body")) continue;
            if (!row.views.isEmpty()) { row.views.get(0).requestFocus(); return; }
        }
        if (footer.getChildCount()>0) footer.getChildAt(footer.getChildCount()-1).requestFocus();
    }
    private void enter() {
        if (!TvMotion.enabled(this)) return;
        body.setAlpha(0); body.setTranslationY(dp(8)); body.animate().alpha(1).translationY(0).setDuration(220).setInterpolator(TvUi.EASE_DRAWER).start();
    }
    void refreshChoices() { render(false); }
    void open(Class<?> type) { child=true; pendingLights=type==LightsActivity.class; startActivity(new Intent(this,type)); }
    void external(Intent intent) { child=true; pendingLights=false; SystemLinks.open(this,intent); }
    TextView button(String text,Runnable action) {
        TextView button=TvUi.button(this,text,palette,action,14);
        button.setBackground(GlassMaterial.surface(this,palette,14,false)); return button;
    }
    void note(String text) { TextView line=TvUi.text(this,text,14,palette.muted); line.setLineSpacing(dp(3),1); body.addView(line); TvUi.gap(body,12); }
    void option(String title,String detail,Runnable action) {
        LinearLayout row=TvUi.column(this); row.setPadding(dp(15),dp(13),dp(15),dp(13)); row.setMinimumHeight(dp(54));
        row.addView(TvUi.title(this,title,16,palette.text));
        if (!detail.isEmpty()) { TvUi.gap(row,4); row.addView(TvUi.text(this,detail,14,palette.muted)); }
        row.setBackground(GlassMaterial.card(this,palette,16)); TvUi.focus(row,palette,16,1,0,null); row.setContentDescription(title+". "+detail);
        row.setOnClickListener(v -> action.run()); body.addView(row,TvUi.params(this,-1,-2,0)); TvUi.gap(body,10);
    }
    private List<FocusNavigator.Row> navigationRows() {
        List<FocusNavigator.Row> rows=new ArrayList<>(); FocusNavigator.add(rows,"setup-side",sidebar); FocusNavigator.add(rows,"setup-body",body); FocusNavigator.add(rows,"setup-footer",footer); return rows;
    }
    private void done() { SetupState.complete(this); finish(); }
    private android.graphics.drawable.Drawable background() {
        GradientDrawable base=new GradientDrawable(GradientDrawable.Orientation.TL_BR,palette.light
                ? new int[]{0xFFF2F3F5,0xFFE2E6EB,0xFFF5F5F7} : new int[]{0xFF292D32,0xFF12151A,0xFF23262D});
        GradientDrawable top=new GradientDrawable(); top.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        top.setGradientCenter(.9f,.05f); top.setGradientRadius(dp(420)); top.setColors(new int[]{palette.light ? 0x99FFFFFF : 0x337E8989,0x00000000});
        GradientDrawable bottom=new GradientDrawable(); bottom.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        bottom.setGradientCenter(.85f,1f); bottom.setGradientRadius(dp(380)); bottom.setColors(new int[]{palette.light ? 0x55AAB7C4 : 0x334A4F60,0x00000000});
        return new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{base,top,bottom});
    }
    int dp(float value) { return TvUi.dp(this,value); }
    @Override protected void onResume() {
        super.onResume(); if (themes!=null) themes.refresh();
        if (child) {
            child=false;
            if (pendingLights && !new WizStore(this).load().isEmpty()) new TvPreferences(this).widget(HomeWidget.LIGHTS,true);
            pendingLights=false; render(false);
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) { super.onRequestPermissionsResult(request,permissions,results); if (request==NetworkNameAccess.REQUEST) render(false); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) { return navigation!=null && navigation.key(event) || super.dispatchKeyEvent(event); }
    @Override public void onBackPressed() { if (step>0) go(step-1); else done(); }
    @Override protected void onStop() { if (motion!=null) motion.stop(); super.onStop(); }
    @Override protected void onDestroy() { if (themes!=null) themes.close(); if (motion!=null) motion.stop(); super.onDestroy(); }
}
