package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.json.JSONException;
import org.json.JSONObject;

/** Native rendering of the approved living-room composition. Network work is bounded to visible Home. */
final class LivingHome {
    final FrameLayout root;
    final String styleSignature;
    private final Activity activity;
    private final TvPalette palette;
    private final ScrollView scroll;
    private final PageMotion pageMotion;
    private final FocusNavigator navigation;
    private final LinearLayout content;
    private final ImageView backdrop;
    private final BackdropShade shade,lowerShade;
    private final View below;
    private final LivingHeader header;
    private final LivingHero hero;
    private final LivingDock dock;
    private final LivingControls controls;
    private final AppCatalog catalog;
    private final ArtworkCache artwork;
    private final ResumeArtwork resumeArtwork;
    private final StremioStore account;
    private final WeatherRepository weather;
    private final WizStore wizStore;
    private final WizClient wiz=new WizClient();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService network=Executors.newFixedThreadPool(2);
    private final List<Future<?>> requests=new ArrayList<>();
    private List<WizBulb> bulbs;
    private String activeArt="";
    private String catalogSignature="";
    private float shadeTarget=-1,lowerShadeTarget=-1;
    private boolean lowerPage;
    private boolean visible, closed, powerBusy;
    private int generation;
    private final Runnable minute=this::clock;
    static String style(Activity activity) {
        TvPreferences p=new TvPreferences(activity);
        return p.theme()+":"+TvPalette.of(activity).light+":"+p.motion()+":"+p.reduced()+":"+TvAccessibility.exploring(activity)+":"+WeatherLocation.load(activity).signature()+":"+p.widgetSignature();
    }
    LivingHome(Activity activity) {
        this.activity=activity; palette=TvPalette.of(activity); styleSignature=style(activity);
        catalog=new AppCatalog(activity); artwork=new ArtworkCache(activity); resumeArtwork=new ResumeArtwork(activity);
        account=new StremioStore(activity); weather=new WeatherRepository(activity); wizStore=new WizStore(activity); bulbs=wizStore.load();
        root=new FrameLayout(activity);
        FrostedBackdrop.home(root);
        root.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                palette.light ? new int[]{0xFFF7F7F8,0xFFE5E5E8,0xFFF2F2F4} : new int[]{0xFF101113,0xFF222326,0xFF101113}));
        root.setClipChildren(false);
        backdrop=new ImageView(activity); backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP); TvAccessibility.decorative(backdrop);
        shade=new BackdropShade(activity); shade.setAlpha(.4f); TvAccessibility.decorative(shade);
        lowerShade=new BackdropShade(activity,true); lowerShade.setAlpha(0); TvAccessibility.decorative(lowerShade);
        root.addView(backdrop,new FrameLayout.LayoutParams(-1,-1));
        root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        root.addView(lowerShade,new FrameLayout.LayoutParams(-1,-1));
        scroll=new FocusScrollView(activity); scroll.setFillViewport(false); scroll.setVerticalScrollBarEnabled(false); scroll.setClipChildren(false);
        pageMotion=new PageMotion(scroll);
        content=TvUi.column(activity); scroll.addView(content); root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        header=new LivingHeader(activity,this::inputs);
        header.root.setMinimumHeight(dp(55));
        hero=new LivingHero(activity,() -> open(StremioLinkActivity.class),this::art);
        dock=new LivingDock(activity);
        content.addView(new HomeStage(activity,header.root,hero.root,dock.root),TvUi.params(activity,-1,-2,0));
        LinearLayout body=TvUi.column(activity);
        body.setMinimumHeight(dp(355)); content.addView(body);
        TvUi.gap(body,15); controls=new LivingControls(activity); addSection(body,controls.root); TvUi.gap(body,40);
        body.setVisibility(controls.hasVisibleWidgets() ? View.VISIBLE : View.GONE);
        below=new IconView(activity,"down",palette.muted); TvAccessibility.decorative(below);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(18),dp(18),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL); bp.bottomMargin=dp(2); root.addView(below,bp);
        below.setVisibility(controls.hasVisibleWidgets() ? View.VISIBLE : View.GONE);
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldView,newView) -> focusPage(newView));
        root.addOnLayoutChangeListener((view,l,t,r,b,oldL,oldT,oldR,oldB) -> {
            if (r-l!=oldR-oldL || b-t!=oldB-oldT) focusPage(activity.getCurrentFocus());
        });
        navigation=new FocusNavigator(activity,this::navigationRows);
        navigation.reachable(dock::reachable);
        refreshApps(); controls.weather.show(weather.cached()); header.weather(weather.cached()); renderLights();
        hero.show(account.cached(),account.linked(),false);
        navigation.wire();
    }
    private void addSection(LinearLayout parent,View section) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(dp(52),0,dp(52),0); parent.addView(section,p);
    }
    private void refreshApps() {
        List<AppAdapter.Entry> installed=catalog.reload(), featured=catalog.favorites();
        StringBuilder identity=new StringBuilder();
        for (AppAdapter.Entry app:installed) identity.append(app.component.flattenToString()).append(app.label).append(';');
        for (AppAdapter.Entry app:featured) identity.append("dock:").append(app.pkg);
        if (identity.toString().equals(catalogSignature)) return;
        catalogSignature=identity.toString();
        dock.render(featured,catalog,() -> open(AppsActivity.class));
    }
    // Playback remains observable under translucent panels, while Home is STARTED but not RESUMED.
    void start() { if (!closed) header.start(); }
    void stop() { header.stop(); }
    void resume() {
        if (closed || visible) return;
        FrostedBackdrop.home(root);
        visible=true; generation++; activeArt=""; refreshApps(); clock(); bulbs=wizStore.load();
        hero.show(account.cached(),account.linked(),false); renderLights(); loadWeather(); loadResume(); loadLights(); loadStatus();
        navigation.wire(); int request=generation;
        root.post(() -> { if (current(request)) { navigation.restore(); focusPage(activity.getCurrentFocus()); } });
    }
    void pause() {
        navigation.remember(); visible=false; pageMotion.stop(); dock.stop();
        generation++; powerBusy=false; main.removeCallbacksAndMessages(null);
        for (Future<?> task:requests) task.cancel(true); requests.clear();
    }
    void close() { if (closed) return; pause(); stop(); closed=true; network.shutdownNow(); artwork.close(); }
    static final class ViewState {
        final String app,title;
        final int railStart,scrollY;
        final boolean lowerPage;
        final FocusBookmark focus;
        ViewState(LivingHome home) {
            app=home.dock.selectedPackage(); railStart=home.dock.windowStart(); title=home.hero.selectedId();
            scrollY=home.scroll.getScrollY(); lowerPage=home.lowerPage;
            focus=FocusBookmark.capture(home.activity.getCurrentFocus(),home.root);
        }
    }
    ViewState saveViewState() { return new ViewState(this); }
    void restoreViewState(ViewState state) {
        lowerPage=state.lowerPage && controls.hasVisibleWidgets(); hero.selectId(state.title);
        root.post(() -> {
            if (closed) return;
            dock.restoreSelection(state.app,state.railStart);
            state.focus.restore(root); lowerPage=state.lowerPage && controls.hasVisibleWidgets(); below.setRotation(lowerPage ? 180 : 0); pageMotion.jumpTo(lowerPage ? state.scrollY : 0);
            backdropDepth(false);
            navigation.remember();
        });
    }
    void home() { dock.stop(); lowerPage=false; backdropDepth(true); navigation.home(); pageMotion.to(0); }
    void moveApp(String pkg) { root.post(() -> dock.moveApp(pkg)); }
    void voiceResult(int result,Intent data) { controls.search.result(result,data); }
    void settings() { open(QuickSettingsActivity.class); }
    private void inputs() { open(InputsActivity.class); }
    private void open(Class<?> type) { activity.startActivity(new Intent(activity,type)); }
    private void clock() {
        if (!visible) return; header.tick(); main.removeCallbacks(minute);
        main.postDelayed(minute,60000-System.currentTimeMillis()%60000);
    }
    private void loadWeather() {
        int request=generation;
        requests.add(network.submit(() -> {
            WeatherData data;
            try { data=weather.load(false); } catch (IOException error) { data=weather.cached(); }
            WeatherData result=data;
            main.post(() -> { if (!current(request)) return; controls.weather.show(result); header.weather(result); });
        }));
    }
    private void loadResume() {
        if (!account.linked()) return;
        int request=generation;
        requests.add(network.submit(() -> {
            List<ResumeItem> data; boolean offline=false;
            try { data=new StremioRepository(activity).load(); } catch (IOException error) { data=account.cached(); offline=true; }
            List<ResumeItem> result=data; boolean failed=offline;
            main.post(() -> { if (current(request)) { hero.show(result,account.linked(),failed); navigation.wire(); } });
        }));
    }
    private void art(ResumeItem item) {
        activeArt=item==null ? "" : item.type+":"+item.id; backdropDepth(false);
        if (item==null) { backdrop.setTag(""); backdrop.setImageDrawable(null); FrostedBackdrop.artwork(null); return; }
        String id=activeArt;
        String cached=resumeArtwork.cached(item);
        artwork.load(backdrop,cached,true);
        if (!visible || !cached.isEmpty()) return;
        int request=generation;
        requests.add(network.submit(() -> {
            String path;
            try { path=resumeArtwork.load(item); } catch (IOException error) { path=""; }
            String result=path;
            main.post(() -> { if (current(request) && id.equals(activeArt)) artwork.load(backdrop,result,true); });
        }));
    }
    private void loadLights() {
        if (!new TvPreferences(activity).widget(HomeWidget.LIGHTS)) return;
        int request=generation; List<WizBulb> known=new ArrayList<>(bulbs);
        requests.add(network.submit(() -> {
            for (WizBulb bulb:known) { if (Thread.currentThread().isInterrupted()) return; wiz.refresh(bulb); }
            main.post(() -> { if (current(request)) { bulbs=known; powerBusy=false; renderLights(); } });
        }));
    }
    private void renderLights() { controls.lights(bulbs,this::toggle,this::rename); navigation.wire(); }
    private void toggle(WizBulb bulb) {
        if (powerBusy || !visible) return;
        if (!bulb.reachable) { powerBusy=true; loadLights(); return; }
        powerBusy=true; int request=generation;
        requests.add(network.submit(() -> {
            boolean ok=false;
            if (bulb.reachable) {
                try { ok=wiz.apply(bulb,new JSONObject().put("state",!bulb.pilot.optBoolean("state"))); }
                catch (JSONException ignored) { }
            } else wiz.refresh(bulb);
            boolean confirmed=ok;
            main.post(() -> {
                if (!current(request)) return; powerBusy=false; renderLights();
                if (!confirmed) Toast.makeText(activity,activity.getString(R.string.hc_command_failed),Toast.LENGTH_SHORT).show();
            });
        }));
    }
    private void rename(WizBulb bulb) {
        if (powerBusy) return;
        TvTextDialog.show(activity,TvUi.label(activity,"Rename light","Renombrar luz"),bulb.alias,
                TvUi.label(activity,"Light name","Nombre de la luz"),false,48,name -> {
            if (name.isEmpty()) return TvUi.label(activity,"Enter a name","Ingresá un nombre");
            bulb.alias=name; wizStore.save(bulbs); renderLights(); return null;
        });
    }
    private boolean current(int request) { return visible && request==generation; }
    void refreshConnections() { if (visible) loadStatus(); }
    private void loadStatus() {
        if (!new TvPreferences(activity).widget(HomeWidget.CONNECTIONS)) return;
        int request=generation;
        requests.add(network.submit(() -> {
            ConnectionStatus status=ConnectionStatus.read(activity);
            main.post(() -> { if (current(request)) controls.status(status); });
        }));
    }
    private void focusPage(View view) {
        if (!visible || view==null || !within(view,content)) return;
        dock.select(view);
        boolean wasLower=lowerPage;
        if (within(view,controls.root)) lowerPage=true;
        if (within(view,hero.root) || within(view,header.root)) lowerPage=false;
        if (wasLower!=lowerPage) backdropDepth(true);
        below.setRotation(lowerPage ? 180 : 0);
        if (wasLower==lowerPage && pageMotion.fits(view,content,dp(16),dp(24))) return;
        int target=lowerPage ? Math.max(0,PageMotion.top(dock.root,content)-dp(22)) : 0;
        pageMotion.to(pageMotion.visibleTarget(view,content,target,dp(16),dp(24)));
    }
    private void backdropDepth(boolean animate) {
        float alpha=lowerPage ? 0f : (activeArt.isEmpty() ? .4f : 1f);
        float lowerAlpha=lowerPage ? 1f : 0f;
        if (alpha==shadeTarget && lowerAlpha==lowerShadeTarget) return;
        shadeTarget=alpha; lowerShadeTarget=lowerAlpha;
        fadeShade(shade,alpha,animate); fadeShade(lowerShade,lowerAlpha,animate);
    }
    private void fadeShade(View view,float alpha,boolean animate) {
        view.animate().cancel();
        if (animate && TvMotion.enabled(activity)) view.animate().alpha(alpha).setDuration(240).setInterpolator(TvUi.EASE_DRAWER).start();
        else view.setAlpha(alpha);
    }
    boolean key(KeyEvent event) {
        if (dock.key(event,activity.getCurrentFocus())) { navigation.reset(); return true; }
        if (event.getAction()!=KeyEvent.ACTION_DOWN) return false;
        int code=event.getKeyCode();
        if (globalKey(code)) return true;
        return navigation.key(event);
    }
    private boolean globalKey(int code) {
        if (code==KeyEvent.KEYCODE_SETTINGS || code==KeyEvent.KEYCODE_NOTIFICATION) { settings(); return true; }
        if (code==KeyEvent.KEYCODE_TV_INPUT || code==KeyEvent.KEYCODE_TV) { inputs(); return true; }
        return false;
    }
    private List<FocusNavigator.Row> navigationRows() {
        List<FocusNavigator.Row> rows=new ArrayList<>();
        header.navigation(rows); hero.navigation(rows); dock.navigation(rows); controls.navigation(rows); return rows;
    }
    static boolean within(View view,View ancestor) {
        if (view==null) return false;
        if (view==ancestor) return true;
        android.view.ViewParent parent=view.getParent();
        while (parent instanceof View) { if (parent==ancestor) return true; parent=parent.getParent(); } return false;
    }
    private int dp(float value) { return TvUi.dp(activity,value); }
}
