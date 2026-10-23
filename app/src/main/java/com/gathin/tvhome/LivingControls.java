package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.media.tv.TvContract;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared twelve-dp gutters align both halves with three app banners each. */
final class LivingControls {
    final LinearLayout root;
    final ForecastCard weather;
    final SearchControl search;
    private final Activity activity;
    private final TvPalette palette;
    private final LinearLayout lights;
    private final Map<String,LightCard> lightRows=new LinkedHashMap<>();
    private final TextView wifiValue,audioValue;
    private String lightOrder="";
    private View first;
    LivingControls(Activity activity) {
        this.activity=activity; palette=TvPalette.of(activity); root=TvUi.column(activity);
        LinearLayout upper=TvUi.row(activity); upper.setGravity(Gravity.TOP); upper.setBaselineAligned(false);
        LinearLayout inputs=TvUi.column(activity); inputs.addView(TvUi.title(activity,TvUi.label(activity,"Inputs","Entradas"),17,palette.text));
        TvUi.gap(inputs,11); LinearLayout sources=TvUi.row(activity); inputs.addView(sources); addInputs(sources);
        search=new SearchControl(activity); LinearLayout query=TvUi.column(activity);
        query.addView(TvUi.title(activity,TvUi.label(activity,"Search","Buscar"),17,palette.text)); TvUi.gap(query,11); query.addView(search.root);
        half(upper,inputs,true); half(upper,query,false); root.addView(upper); TvUi.gap(root,22);
        LinearLayout widgets=TvUi.row(activity); widgets.setGravity(Gravity.TOP); widgets.setBaselineAligned(false);
        weather=new ForecastCard(activity,true); TvUi.focus(weather,palette,12,1.01f,0,null);
        weather.setOnClickListener(v -> open(WeatherActivity.class)); half(widgets,weather,true);
        LinearLayout right=TvUi.row(activity); right.setGravity(Gravity.TOP); right.setBaselineAligned(false);
        LinearLayout lighting=card();
        LinearLayout heading=TvUi.row(activity); heading.addView(TvUi.title(activity,TvUi.label(activity,"Lights","Luces"),17,palette.text),new LinearLayout.LayoutParams(0,-2,1));
        heading.addView(new IconView(activity,"bulb",palette.muted),TvUi.params(activity,19,22,0)); lighting.addView(heading); TvUi.gap(lighting,13);
        lights=TvUi.column(activity); lighting.addView(lights); half(right,lighting,true);
        LinearLayout connections=card(); connections.setPadding(dp(12),dp(12),dp(12),dp(12));
        wifiValue=status(connections,"wifi","Wi-Fi",() -> SystemLinks.wifi(activity));
        audioValue=status(connections,"sound",TvUi.label(activity,"Audio","Audio"),() -> SystemLinks.sound(activity));
        status(connections,"grid",activity.getString(R.string.lr_all_apps),() -> open(AppsActivity.class));
        half(right,connections,false); half(widgets,right,false); root.addView(widgets);
        TvPreferences preferences=new TvPreferences(activity);
        pair(upper,inputs,preferences.widget(HomeWidget.INPUTS),query,preferences.widget(HomeWidget.SEARCH));
        pair(right,lighting,preferences.widget(HomeWidget.LIGHTS),connections,preferences.widget(HomeWidget.CONNECTIONS));
        pair(widgets,weather,preferences.widget(HomeWidget.WEATHER),right,right.getVisibility()==View.VISIBLE);
        root.getChildAt(1).setVisibility(upper.getVisibility()==View.VISIBLE && widgets.getVisibility()==View.VISIBLE ? View.VISIBLE : View.GONE);
        root.setVisibility(preferences.anyWidget() ? View.VISIBLE : View.GONE);
    }
    private void pair(LinearLayout row,View left,boolean showLeft,View right,boolean showRight) {
        left.setVisibility(showLeft ? View.VISIBLE : View.GONE);
        right.setVisibility(showRight ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams bounds=(LinearLayout.LayoutParams)left.getLayoutParams();
        bounds.setMarginEnd(showLeft && showRight ? dp(12) : 0); left.setLayoutParams(bounds);
        row.setVisibility(showLeft || showRight ? View.VISIBLE : View.GONE);
    }
    boolean hasVisibleWidgets() { return root.getVisibility()==View.VISIBLE; }
    private void addInputs(LinearLayout row) {
        List<TvInputs.Entry> entries=TvInputs.list(activity);
        for (int i=0;i<Math.min(4,entries.size());i++) {
            TvInputs.Entry input=entries.get(i);
            String label=input.order<100 ? "AV" : input.label;
            View item=source(input.order<100 ? "inputs" : "hdmi",label,() -> SystemLinks.open(activity,
                    new Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.buildChannelUriForPassthroughInput(input.id),"vnd.android.cursor.item/channel")));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); if (i<Math.min(4,entries.size())-1) p.setMarginEnd(dp(12));
            row.addView(item,p); if (first==null) first=item;
        }
        if (entries.isEmpty()) {
            first=source("inputs",TvUi.label(activity,"Inputs","Entradas"),() -> open(InputsActivity.class)); row.addView(first,new LinearLayout.LayoutParams(-1,dp(64)));
        }
    }
    private View source(String icon,String label,Runnable action) {
        LinearLayout item=TvUi.column(activity); item.setGravity(Gravity.CENTER); item.setMinimumHeight(dp(64)); item.setPadding(dp(4),dp(8),dp(4),dp(8)); item.setBackground(GlassMaterial.surface(item,palette,10,false));
        item.addView(new IconView(activity,icon,palette.text),TvUi.params(activity,22,22,0)); TvUi.gap(item,6);
        TextView text=TvUi.text(activity,label,16,palette.text); text.setSingleLine(); text.setGravity(Gravity.CENTER); text.setEllipsize(android.text.TextUtils.TruncateAt.END); TvAccessibility.decorative(text);
        item.addView(text,new LinearLayout.LayoutParams(-1,-2)); item.setContentDescription(label); TvUi.focus(item,palette,10,1.02f,0,null); item.setOnClickListener(v -> action.run()); return item;
    }
    private LinearLayout card() {
        LinearLayout card=TvUi.column(activity); card.setPadding(dp(12),dp(16),dp(12),dp(13)); card.setMinimumHeight(dp(170));
        card.setBackground(GlassMaterial.card(card,palette,12)); return card;
    }
    private void half(LinearLayout row,View view,boolean left) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1); if (left) p.setMarginEnd(dp(12)); row.addView(view,p);
    }
    private TextView status(LinearLayout parent,String icon,String title,Runnable action) {
        if (parent.getChildCount()>0) TvUi.line(parent);
        LinearLayout row=TvUi.row(activity); row.setPadding(dp(10),dp(6),dp(10),dp(6)); row.setMinimumHeight(dp(48));
        row.addView(new IconView(activity,icon,palette.text),TvUi.params(activity,18,21,9));
        LinearLayout labels=TvUi.column(activity);
        boolean all=icon.equals("grid"); labels.addView(TvUi.text(activity,title,all ? 16 : 14,all ? palette.text : palette.muted));
        TextView value=TvUi.text(activity,"—",15,palette.text); value.setSingleLine(); value.setEllipsize(android.text.TextUtils.TruncateAt.END);
        if (!all) { TvUi.gap(labels,2); labels.addView(value); }
        row.addView(labels,new LinearLayout.LayoutParams(0,-2,1)); row.addView(new IconView(activity,"right",palette.muted),TvUi.params(activity,12,16,0));
        TvUi.focus(row,palette,7,1,0,null); row.setContentDescription(title); row.setOnClickListener(v -> action.run()); parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        return value;
    }
    void status(ConnectionStatus status) {
        wifiValue.setText(status.wifi); audioValue.setText(status.audio);
        ((View)wifiValue.getParent().getParent()).setContentDescription("Wi-Fi. "+status.wifi);
        ((View)audioValue.getParent().getParent()).setContentDescription(TvUi.label(activity,"Selected audio output. ","Salida de audio seleccionada. ")+status.audio);
    }
    void lights(List<WizBulb> devices,java.util.function.Consumer<WizBulb> toggle,java.util.function.Consumer<WizBulb> unusedRename) {
        StringBuilder signature=new StringBuilder(); int limit=Math.min(2,devices.size());
        for (int i=0;i<limit;i++) signature.append(devices.get(i).mac).append(':');
        boolean changed=!signature.toString().equals(lightOrder) || lights.getChildCount()==0;
        if (changed) { lights.removeAllViews(); lightRows.clear(); lightOrder=signature.toString(); }
        for (int i=0;i<limit;i++) {
            WizBulb device=devices.get(i); LightCard card=lightRows.get(device.mac);
            if (card==null) { card=new LightCard(activity,device,() -> toggle.accept(device)); lights.addView(card,new LinearLayout.LayoutParams(-1,-2)); lightRows.put(device.mac,card); }
            card.bind(device,() -> toggle.accept(device),null);
        }
        if (changed && devices.isEmpty()) lights.addView(TvUi.text(activity,TvUi.label(activity,"No lights found","Sin luces detectadas"),14,palette.muted));
    }
    void navigation(java.util.List<FocusNavigator.Row> rows) { FocusNavigator.add(rows,"controls",root); }
    void focus() {
        java.util.List<FocusNavigator.Row> rows=new java.util.ArrayList<>(); navigation(rows);
        if (!rows.isEmpty()) rows.get(0).views.get(0).requestFocus();
    }
    private void open(Class<?> type) { activity.startActivity(new Intent(activity,type)); }
    private int dp(float value) { return TvUi.dp(activity,value); }
}
