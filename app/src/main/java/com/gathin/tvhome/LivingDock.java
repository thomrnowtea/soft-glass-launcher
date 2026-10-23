package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A single unboxed rail with six complete banners; the same rail stays above the lower page. */
final class LivingDock {
    final LinearLayout root;
    private final Activity activity;
    private final HorizontalScrollView viewport;
    private final LinearLayout track;
    private final TextView position;
    private final SpringAnimation spring;
    private final List<View> tiles=new ArrayList<>();
    private List<AppAdapter.Entry> apps=new ArrayList<>();
    private AppCatalog catalog;
    private int selected,windowStart,tileWidth,consumedConfirm;
    private float tileExtent;
    private String moving="";
    private long lastRepeat;
    LivingDock(Activity activity) {
        this.activity=activity; TvPalette p=TvPalette.of(activity); root=TvUi.column(activity);
        LinearLayout heading=TvUi.row(activity); heading.setPadding(dp(8),0,dp(8),0);
        heading.addView(TvUi.title(activity,"Apps",17,p.text),new LinearLayout.LayoutParams(0,-2,1));
        position=TvUi.text(activity,"",14,p.muted); heading.addView(position); root.addView(heading);
        viewport=new HorizontalScrollView(activity) {
            @Override protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect rectangle) {
                return TvAccessibility.exploring(getContext()) ? super.computeScrollDeltaToGetChildRectOnScreen(rectangle) : 0;
            }
            @Override public boolean requestChildRectangleOnScreen(View child,android.graphics.Rect rectangle,boolean immediate) { return false; }
        };
        viewport.setHorizontalScrollBarEnabled(false); viewport.setClipToPadding(true); viewport.setClipChildren(true);
        viewport.setOverScrollMode(View.OVER_SCROLL_NEVER);
        viewport.setPadding(dp(8),dp(8),dp(8),dp(8)); viewport.setFocusable(false);
        track=TvUi.row(activity); track.setGravity(Gravity.TOP); viewport.addView(track);
        root.addView(viewport,new LinearLayout.LayoutParams(-1,-2));
        spring=new SpringAnimation(viewport,new FloatPropertyCompat<HorizontalScrollView>("railScroll") {
            @Override public float getValue(HorizontalScrollView view) { return view.getScrollX(); }
            @Override public void setValue(HorizontalScrollView view,float value) { view.scrollTo(Math.round(value),0); }
        },0);
        spring.setSpring(new SpringForce(0).setDampingRatio(1).setStiffness(1100));
        spring.setMinimumVisibleChange(.5f);
    }
    void render(List<AppAdapter.Entry> entries,AppCatalog catalog,Runnable unused) {
        String keep=selectedPackage(); this.catalog=catalog; apps=new ArrayList<>(entries);
        rebuild(keep);
    }
    private void rebuild(String keep) {
        spring.cancel(); track.removeAllViews(); tiles.clear(); selected=0;
        tileExtent=(activity.getResources().getDisplayMetrics().widthPixels-dp(104)+dp(12))/6f;
        tileWidth=Math.round(tileExtent)-dp(12);
        for (int i=0;i<apps.size();i++) add(apps.get(i),i);
        if (apps.isEmpty()) {
            TextView choose=TvUi.button(activity,TvUi.label(activity,"Choose apps","Elegir apps"),TvPalette.of(activity),
                    () -> activity.startActivity(new Intent(activity,AppsActivity.class)));
            track.addView(choose,new LinearLayout.LayoutParams(dp(200),dp(76))); tiles.add(choose);
        }
        for (int i=0;i<apps.size();i++) if (apps.get(i).pkg.equals(keep)) selected=i;
        windowStart=Math.max(0,Math.min(windowStart,Math.max(0,apps.size()-6)));
        viewport.scrollTo(Math.round(windowStart*tileExtent),0); updatePosition();
    }
    private void add(AppAdapter.Entry app,int index) {
        AppTile tile=new AppTile(activity,app,0,Math.round(tileWidth*9f/16/activity.getResources().getDisplayMetrics().density),true,
                () -> { if (!moving.isEmpty()) finishMove(); else catalog.launch(app); });
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        tile.setOnLongClickListener(v -> { if (moving.isEmpty()) moveApp(app.pkg); return true; });
        int width=Math.round((index+1)*tileExtent)-Math.round(index*tileExtent)-dp(12);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(width,-2);
        if (index<apps.size()-1) lp.setMarginEnd(dp(12));
        track.addView(tile,lp); tiles.add(tile);
    }
    void moveApp(String pkg) {
        for (int i=0;i<apps.size();i++) if (apps.get(i).pkg.equals(pkg)) {
            moving=pkg; selected=i; focus(); updatePosition();
            tiles.get(i).announceForAccessibility(TvUi.label(activity,"Left or right to move. OK to finish.","Izquierda o derecha para mover. OK para terminar.")); return;
        }
    }
    boolean key(KeyEvent event,View focused) {
        if (event.getAction()==KeyEvent.ACTION_UP && event.getKeyCode()==consumedConfirm) {
            consumedConfirm=0; return true;
        }
        if (event.getAction()!=KeyEvent.ACTION_DOWN || !contains(focused)) return false;
        int code=event.getKeyCode();
        if (finishKey(event)) return true;
        if (code!=KeyEvent.KEYCODE_DPAD_LEFT && code!=KeyEvent.KEYCODE_DPAD_RIGHT) return false;
        if (event.getRepeatCount()>0 && event.getEventTime()-lastRepeat<75) return true;
        lastRepeat=event.getEventTime();
        moveFocus(code,focused); return true;
    }
    private boolean finishKey(KeyEvent event) {
        if (moving.isEmpty()) return false;
        int code=event.getKeyCode();
        switch (code) {
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_ESCAPE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (event.getRepeatCount()==0) { consumedConfirm=code; finishMove(); }
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN: finishMove(); return false;
            default: return false;
        }
    }
    private void moveFocus(int code,View focused) {
        int index=tiles.indexOf(focused),next=Math.max(0,Math.min(tiles.size()-1,index+(code==KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1)));
        if (next==index) return;
        if (!moving.isEmpty()) reorder(index,next);
        selected=next; tiles.get(next).requestFocus(); select(tiles.get(next));
    }
    private void reorder(int from,int to) {
        Collections.swap(apps,from,to); Collections.swap(tiles,from,to);
        View tile=tiles.get(to); track.removeView(tile); track.addView(tile,to);
        for (int i=0;i<tiles.size();i++) {
            LinearLayout.LayoutParams bounds=(LinearLayout.LayoutParams)tiles.get(i).getLayoutParams();
            bounds.width=Math.round((i+1)*tileExtent)-Math.round(i*tileExtent)-dp(12);
            bounds.setMarginEnd(i<tiles.size()-1 ? dp(12) : 0); tiles.get(i).setLayoutParams(bounds);
        }
    }
    private void finishMove() {
        if (moving.isEmpty()) return;
        List<String> order=new ArrayList<>();
        for (AppAdapter.Entry app:apps) order.add(app.pkg);
        new TvPreferences(activity).saveFavorites(order); moving=""; updatePosition();
    }
    void select(View view) {
        int index=tiles.indexOf(view); if (index<0) return; selected=index;
        if (index<windowStart) windowStart=index;
        if (index>=windowStart+6) windowStart=index-5;
        int target=Math.round(windowStart*tileExtent);
        if (TvMotion.enabled(activity)) spring.animateToFinalPosition(target); else viewport.scrollTo(target,0);
        updatePosition();
    }
    private void updatePosition() {
        String range=apps.isEmpty() ? "0 / 0" : (windowStart+1)+"–"+Math.min(windowStart+6,apps.size())+" / "+apps.size();
        position.setText(moving.isEmpty() ? range : TvUi.label(activity,"Move · OK to finish","Mover · OK para terminar"));
    }
    String selectedPackage() { return selected<apps.size() ? apps.get(selected).pkg : ""; }
    int windowStart() { return windowStart; }
    void restoreSelection(String pkg,int first) {
        for (int i=0;i<apps.size();i++) if (apps.get(i).pkg.equals(pkg)) {
            selected=i; windowStart=Math.max(0,Math.min(first,Math.max(0,apps.size()-6)));
            viewport.scrollTo(Math.round(windowStart*tileExtent),0); updatePosition(); return;
        }
    }
    boolean reachable(View view) {
        int index=tiles.indexOf(view); return index<0 || (index>=windowStart && index<windowStart+6);
    }
    boolean contains(View view) { return LivingHome.within(view,root); }
    void navigation(List<FocusNavigator.Row> rows) { FocusNavigator.add(rows,"dock",track); }
    void focus() { if (!tiles.isEmpty()) { View view=tiles.get(Math.min(selected,tiles.size()-1)); view.requestFocus(); select(view); } }
    void stop() { finishMove(); spring.cancel(); }
    private int dp(float value) { return TvUi.dp(activity,value); }
}
