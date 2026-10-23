package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Unboxed resume typography over the full-screen backdrop; no sample or trending titles. */
final class LivingHero {
    final LinearLayout root;
    private final Activity activity;
    private final TvPalette palette;
    private final TextView title, detail, action;
    private final TextView section;
    private final ProgressBar progress;
    private final LinearLayout pages;
    private final LinearLayout dots;
    private final TextView pageNumber;
    private final Runnable connect;
    private final java.util.function.Consumer<ResumeItem> artwork;
    private List<ResumeItem> items=new ArrayList<>();
    private int selected;
    private boolean linked;
    LivingHero(Activity activity,Runnable connect,java.util.function.Consumer<ResumeItem> artwork) {
        this.activity=activity; this.connect=connect; this.artwork=artwork; palette=TvPalette.of(activity);
        root=TvUi.column(activity);
        section=TvUi.text(activity,"",14,palette.muted); root.addView(section);
        TvUi.gap(root,4);
        title=TvUi.title(activity,"Stremio",40,palette.text); title.setMaxLines(2); title.setGravity(Gravity.CENTER_VERTICAL); title.setEllipsize(android.text.TextUtils.TruncateAt.END); TvAccessibility.heading(title);
        // Reserve both lines using the actual font metrics, including the owner's font scale.
        title.setLetterSpacing(-.02f); root.addView(title,new LinearLayout.LayoutParams(-1,title.getLineHeight()*2));
        TvUi.gap(root,4);
        detail=TvUi.text(activity,"",16,palette.muted); detail.setSingleLine(); detail.setEllipsize(android.text.TextUtils.TruncateAt.END);
        root.addView(detail,new LinearLayout.LayoutParams(-1,-2));
        TvUi.gap(root,10);
        progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(palette.text));
        progress.setContentDescription(TvUi.label(activity,"Playback progress","Progreso de reproducción"));
        progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(palette.line));
        root.addView(progress,TvUi.params(activity,170,2,0)); TvUi.gap(root,14);
        action=TvUi.title(activity,"",16,palette.onAccent);
        action.setGravity(Gravity.CENTER); action.setPadding(dp(18),0,dp(18),0);
        action.setBackground(GlassMaterial.emphasis(action,palette,19,true));
        if (palette.light) action.setTextColor(0xFFFFFFFF);
        TvUi.focus(action,palette,19,null); action.setOnClickListener(v -> open());
        root.addView(action,TvUi.params(activity,-2,48,0)); TvUi.gap(root,6);
        pages=TvUi.row(activity); root.addView(pages,TvUi.params(activity,-1,48,0));
        pages.addView(TvUi.iconButton(activity,"left",TvUi.label(activity,"Previous title","Título anterior"),false,() -> step(-1)),TvUi.params(activity,48,48,7));
        dots=TvUi.row(activity); pages.addView(dots);
        pages.addView(TvUi.iconButton(activity,"right",TvUi.label(activity,"Next title","Título siguiente"),false,() -> step(1)),TvUi.params(activity,48,48,8));
        pageNumber=TvUi.text(activity,"",14,palette.muted); pages.addView(pageNumber); pages.setVisibility(View.INVISIBLE);
    }
    void show(List<ResumeItem> items,boolean linked,boolean offline) {
        section.setText(TvUi.label(activity,linked ? "Continue Watching" : "Home",linked ? "Seguir viendo" : "Inicio"));
        String previous=this.items.isEmpty() ? "" : this.items.get(selected).id;
        this.items=items; this.linked=linked; selected=0;
        for (int i=0;i<items.size();i++) if (items.get(i).id.equals(previous)) selected=i;
        if (items.isEmpty()) {
            empty(linked,offline); return;
        }
        render();
        if (offline) detail.append(TvUi.label(activity," · Offline"," · Sin conexión"));
    }
    private void empty(boolean linked,boolean offline) {
        title.setText(linked ? TvUi.label(activity,"No unfinished titles","Sin títulos pendientes") : TvUi.label(activity,"Your apps","Tus apps"));
        title.setContentDescription(title.getText()); detail.setText(emptyDescription(linked,offline));
        action.setText(TvUi.label(activity,linked ? "Open Stremio" : "All apps",linked ? "Abrir Stremio" : "Todas las apps"));
        boolean paginationFocused=LivingHome.within(activity.getCurrentFocus(),pages);
        progress.setVisibility(View.INVISIBLE); pages.setVisibility(View.INVISIBLE); artwork.accept(null);
        if (paginationFocused) action.requestFocus();
    }
    private String emptyDescription(boolean linked,boolean offline) {
        if (!linked) return TvUi.label(activity,"Hold an app below to move it.","Mantené una app de abajo para moverla.");
        return TvUi.label(activity,offline ? "Could not sync. Open Stremio or try again later." : "Your synced Continue Watching list is empty.",
                offline ? "No se pudo sincronizar. Abrí Stremio o reintentá más tarde." : "Tu lista sincronizada de Seguir viendo está vacía.");
    }
    private void render() {
        ResumeItem item=items.get(selected);
        title.setText(item.name);
        title.setContentDescription(item.name);
        long remaining=item.remainingMinutes();
        String time=remaining>0 ? TvUi.label(activity,remaining+" min remaining","Faltan "+remaining+" min")
                : TvUi.label(activity,"Progress saved in Stremio","Progreso guardado en Stremio");
        detail.setText(episode(item) + time);
        progress.setVisibility(item.duration>0 ? View.VISIBLE : View.INVISIBLE); progress.setProgress(item.progress());
        action.setText("▶  " + TvUi.label(activity,"Continue in Stremio","Continuar en Stremio"));
        pagination(); artwork.accept(item);
    }
    private String episode(ResumeItem item) {
        if (!item.type.equals("series")) return "";
        String[] parts=item.video.split(":");
        if (parts.length!=3 || !parts[1].matches("[0-9]+") || !parts[2].matches("[0-9]+")) return "";
        return "S"+parts[1]+" · E"+parts[2]+"   ·   ";
    }
    private void pagination() {
        pages.setVisibility(items.size()<2 ? View.INVISIBLE : View.VISIBLE); dots.removeAllViews();
        if (items.size()<2) return;
        int start=Math.max(0,Math.min(selected-4,items.size()-9));
        for (int i=start;i<Math.min(items.size(),start+9);i++) {
            View dot=new View(activity); dot.setBackground(TvUi.shape(activity,i==selected ? palette.text : palette.line,0,3));
            dots.addView(dot,TvUi.params(activity,i==selected ? 16 : 3,3,5));
        }
        pageNumber.setText((selected+1)+"/"+items.size());
    }
    private void step(int delta) {
        if (items.isEmpty()) return;
        selected=(selected+delta+items.size())%items.size(); render();
    }
    String selectedId() { return items.isEmpty() ? "" : items.get(selected).id; }
    void selectId(String id) {
        for (int i=0;i<items.size();i++) if (items.get(i).id.equals(id)) { selected=i; render(); return; }
    }
    private void open() {
        if (!linked) { activity.startActivity(new Intent(activity,AppsActivity.class)); return; }
        Intent intent=items.isEmpty() ? activity.getPackageManager().getLeanbackLaunchIntentForPackage("com.stremio.one")
                : new Intent(Intent.ACTION_VIEW,items.get(selected).detail()).setPackage("com.stremio.one");
        if (intent==null) intent=activity.getPackageManager().getLaunchIntentForPackage("com.stremio.one");
        try { if (intent!=null) activity.startActivity(intent); else unavailable(); }
        catch (ActivityNotFoundException error) { unavailable(); }
    }
    private void unavailable() { android.widget.Toast.makeText(activity,TvUi.label(activity,"Stremio is not available","Stremio no está disponible"),android.widget.Toast.LENGTH_SHORT).show(); }
    private int dp(float value) { return TvUi.dp(activity,value); }
    void navigation(java.util.List<FocusNavigator.Row> rows) {
        FocusNavigator.add(rows,"hero",action);
        FocusNavigator.add(rows,"hero-pages",pages);
    }
}
