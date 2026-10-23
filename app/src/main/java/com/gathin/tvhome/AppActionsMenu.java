package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/** A small menu anchored to its app, preserving the directory's focus and scroll position. */
final class AppActionsMenu {
    private final Activity activity;
    private final AppAdapter.Entry app;
    private final View anchor;
    private final LinearLayout content;
    private final PopupWindow popup;

    static void show(Activity activity,AppAdapter.Entry app,View anchor) {
        new AppActionsMenu(activity,app,anchor).show();
    }
    private AppActionsMenu(Activity activity,AppAdapter.Entry app,View anchor) {
        this.activity=activity; this.app=app; this.anchor=anchor;
        TvPalette palette=TvPalette.of(activity);
        content=TvUi.column(activity); content.setPadding(dp(12),dp(14),dp(12),dp(12));
        content.setBackground(GlassMaterial.surface(content,palette,16,true));
        TextView heading=TvUi.title(activity,app.label,18,palette.text);
        heading.setPadding(dp(10),0,dp(10),dp(8)); heading.setMaxLines(2); TvAccessibility.heading(heading);
        content.addView(heading);
        popup=new PopupWindow(content,dp(248),-2,true);
        popup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setOutsideTouchable(true); popup.setElevation(dp(8)); popup.setAnimationStyle(0);
        popup.setOnDismissListener(() -> anchor.post(() -> {
            if (anchor.isAttachedToWindow() && anchor.isShown()) anchor.requestFocus();
        }));
        boolean onHome=new TvPreferences(activity).homeAppVisible(app.pkg);
        action(TvUi.label(activity,onHome ? "Remove from Home" : "Add to Home",onHome ? "Quitar de Inicio" : "Agregar a Inicio"),() -> pin(!onHome));
        action(TvUi.label(activity,"Details","Detalles"),() -> AppDetails.open(activity,app.pkg));
    }
    private void pin(boolean visible) {
        TvPreferences preferences=new TvPreferences(activity);
        if (visible) preferences.showHomeApp(app.pkg,Config.load(activity).dockApps);
        else preferences.hideHomeApp(app.pkg,Config.load(activity).dockApps);
        anchor.announceForAccessibility(TvUi.label(activity,visible ? "Added to slider" : "Removed from slider",visible ? "Agregada al slider" : "Quitada del slider"));
    }
    private void action(String label,Runnable selected) {
        TextView item=TvUi.plain(activity,label,() -> { popup.dismiss(); selected.run(); });
        item.setContentDescription(app.label+". "+label);
        item.setOnKeyListener((view,key,event) -> menuKey(view,key,event));
        content.addView(item,new LinearLayout.LayoutParams(-1,-2));
    }
    private boolean menuKey(View view,int key,KeyEvent event) {
        if (event.getAction()!=KeyEvent.ACTION_DOWN) return false;
        if (key==KeyEvent.KEYCODE_DPAD_LEFT || key==KeyEvent.KEYCODE_DPAD_RIGHT) return true;
        int index=content.indexOfChild(view);
        if (key==KeyEvent.KEYCODE_DPAD_UP) return move(Math.max(1,index-1));
        if (key==KeyEvent.KEYCODE_DPAD_DOWN) return move(Math.min(content.getChildCount()-1,index+1));
        return false;
    }
    private boolean move(int index) { content.getChildAt(index).requestFocus(); return true; }
    private void show() {
        Rect frame=new Rect(); anchor.getWindowVisibleDisplayFrame(frame);
        int width=Math.min(dp(248),frame.width()-dp(24)); popup.setWidth(width);
        content.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.UNSPECIFIED);
        int[] location=new int[2]; anchor.getLocationOnScreen(location);
        int x=Math.max(frame.left+dp(12),Math.min(location[0],frame.right-width-dp(12)));
        int y=Math.max(frame.top+dp(12),Math.min(location[1],frame.bottom-content.getMeasuredHeight()-dp(12)));
        popup.showAtLocation(activity.getWindow().getDecorView(),Gravity.NO_GRAVITY,x,y);
        content.getChildAt(1).requestFocus();
    }
    private int dp(float value) { return TvUi.dp(activity,value); }
}
