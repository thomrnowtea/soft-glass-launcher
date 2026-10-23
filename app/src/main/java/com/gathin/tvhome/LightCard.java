package com.gathin.tvhome;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Quick power button with its name, state icon and accessible status. */
final class LightCard extends LinearLayout {
    private final TextView power;
    private final View pencil;
    private final TvPalette palette;
    LightCard(Context context,WizBulb device,Runnable action) { this(context,device,action,null); }
    LightCard(Context context,WizBulb device,Runnable action,Runnable rename) {
        super(context); palette=TvPalette.of(context); setGravity(Gravity.CENTER_VERTICAL);
        setPadding(0,dp(3),0,dp(3));
        power=TvUi.button(context,device.alias,palette,action,20); power.setSingleLine(); power.setEllipsize(TextUtils.TruncateAt.END);
        power.setPadding(dp(12),0,dp(12),0); addView(power,new LayoutParams(0,dp(48),1));
        pencil=rename==null ? null : TvUi.iconButton(context,"pencil",TvUi.label(context,"Rename ","Renombrar ")+device.alias,false,rename);
        if (pencil!=null) { LayoutParams params=new LayoutParams(dp(48),dp(48)); params.setMarginStart(dp(8)); addView(pencil,params); }
        show(device,false);
    }
    void bind(WizBulb device,Runnable action,Runnable rename) {
        power.setOnClickListener(v -> action.run());
        if (pencil!=null) pencil.setOnClickListener(v -> rename.run()); show(device,false);
    }
    void show(WizBulb device,boolean busy) {
        boolean on=device.reachable && device.pilot.optBoolean("state");
        String state=busy ? TvUi.label(getContext(),"Changing…","Cambiando…")
                : getContext().getString(device.reachable ? (on ? R.string.hc_on : R.string.hc_off) : R.string.hc_unknown);
        power.setText(device.alias);
        power.setBackground(GlassMaterial.emphasis(power,palette,20,on));
        int foreground=on ? (palette.light ? 0xFFFFFFFF : palette.onAccent) : palette.text;
        power.setTextColor(foreground);
        TvUi.buttonIcons(power,0,device.reachable ? (on ? R.drawable.lr_power_on : R.drawable.lr_power_off) : R.drawable.lr_bulb,foreground);
        power.setAlpha(busy ? .65f : 1); power.setClickable(!busy);
        String action=device.reachable ? TvUi.label(getContext(),on ? "Turn off" : "Turn on",on ? "Apagar" : "Encender")
                : TvUi.label(getContext(),"Check connection","Comprobar conexión");
        TvAccessibility.power(power,device.alias,state,action,on,device.reachable,busy);
        if (pencil!=null) pencil.setContentDescription(TvUi.label(getContext(),"Rename ","Renombrar ")+device.alias);
    }
    private int dp(float value) { return TvUi.dp(getContext(),value); }
}
