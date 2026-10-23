package com.gathin.tvhome;

import android.content.Intent;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;

/** Six shortcuts in two spatial columns, matching the approved HTML sheet. */
public final class QuickSettingsActivity extends ControlPanelActivity {
    @Override protected int panelWidth() { return 320; }
    @Override protected void buildContent() { header(getString(R.string.settings)); status.setVisibility(android.view.View.GONE);
        pair("wifi","Wi-Fi",() -> SystemLinks.wifi(this),"sound",TvUi.label(this,"Sound","Sonido"),() -> SystemLinks.sound(this));
        pair("picture",TvUi.label(this,"Picture","Imagen"),() -> open(new Intent(Settings.ACTION_DISPLAY_SETTINGS)),
                "inputs",TvUi.label(this,"Inputs","Entradas"),() -> open(new Intent(this,InputsActivity.class)));
        pair("settings",TvUi.label(this,"Customize","Personalizar"),() -> open(new Intent(this,LauncherSettingsActivity.class)),
                "accessibility",TvUi.label(this,"Accessibility","Accesibilidad"),() -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        TvUi.gap(content,15); TvUi.line(content); TvUi.gap(content,8);
        content.addView(TvUi.plain(this,TvUi.label(this,"All settings  ›","Todos los ajustes  ›"),() -> open(new Intent(Settings.ACTION_SETTINGS))));
    }
    private void pair(String a,String labelA,Runnable actionA,String b,String labelB,Runnable actionB) {
        LinearLayout row=TvUi.row(this); row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,-2,1); first.setMarginEnd(dp(9));
        row.addView(tile(a,labelA,actionA),first); row.addView(tile(b,labelB,actionB),new LinearLayout.LayoutParams(0,-2,1));
        content.addView(row); TvUi.gap(content,9);
    }
    private android.view.View tile(String icon,String label,Runnable action) {
        LinearLayout tile=TvUi.column(this); tile.setMinimumHeight(dp(86)); tile.setPadding(dp(13),dp(12),dp(13),dp(12));
        tile.setBackground(GlassMaterial.surface(tile,palette,11,false)); tile.addView(new IconView(this,icon,palette.text),TvUi.params(this,22,22,0));
        TvUi.gap(tile,12); android.widget.TextView text=TvUi.text(this,label,16,palette.text); text.setMaxLines(2); tile.addView(text);
        tile.setContentDescription(label); TvUi.focus(tile,palette,11,1.02f,0,null); tile.setOnClickListener(v -> action.run()); return tile;
    }
    private void open(Intent intent) { SystemLinks.open(this,intent); }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); finish(); }
}
