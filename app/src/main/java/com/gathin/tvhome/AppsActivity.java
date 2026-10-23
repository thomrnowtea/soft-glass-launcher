package com.gathin.tvhome;

import android.view.Gravity;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.Collator;
import java.util.List;

/** The approved alphabetical directory: compact icon rows in a two-column side sheet. */
public final class AppsActivity extends ControlPanelActivity {
    @Override protected int panelWidth() { return 320; }
    @Override protected void buildContent() {
        header(R.string.lr_all_apps);
        status.setText(TvUi.label(this,"Hold an app for options","Mantené una app para ver opciones"));
        AppCatalog catalog=new AppCatalog(this);
        List<AppAdapter.Entry> apps=catalog.reload();
        Collator alphabet=Collator.getInstance(getResources().getConfiguration().getLocales().get(0));
        apps.sort((a,b) -> alphabet.compare(a.label,b.label));
        if (apps.isEmpty()) status.setText(TvUi.label(this,"No apps available","No hay apps disponibles"));
        for (int i=0;i<apps.size();i+=2) {
            LinearLayout row=TvUi.row(this); row.setGravity(Gravity.TOP);
            LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,-2,1); first.setMarginEnd(dp(9));
            AppAdapter.Entry left=apps.get(i); row.addView(entry(left,() -> catalog.launch(left)),first);
            if (i+1<apps.size()) {
                AppAdapter.Entry right=apps.get(i+1);
                row.addView(entry(right,() -> catalog.launch(right)),new LinearLayout.LayoutParams(0,-2,1));
            } else row.addView(new android.view.View(this),new LinearLayout.LayoutParams(0,1,1));
            content.addView(row,new LinearLayout.LayoutParams(-1,-2)); TvUi.gap(content,6);
        }
    }
    private LinearLayout entry(AppAdapter.Entry app,Runnable launch) {
        LinearLayout row=TvUi.row(this); row.setMinimumHeight(dp(56)); row.setPadding(dp(8),dp(8),dp(8),dp(8));
        FrameLayout mark=new FrameLayout(this); mark.setBackground(GlassMaterial.surface(mark,palette,6,false));
        ImageView icon=new ImageView(this); icon.setScaleType(ImageView.ScaleType.FIT_CENTER); icon.setImageDrawable(app.icon);
        TvAccessibility.decorative(icon); TvAccessibility.decorative(mark);
        mark.addView(icon,new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER)); row.addView(mark,TvUi.params(this,27,27,8));
        TextView name=TvUi.text(this,app.label,16,palette.text); name.setMaxLines(2); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TvAccessibility.decorative(name); row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        row.setContentDescription(app.label); TvUi.focus(row,palette,11,1,0,null);
        row.setOnClickListener(v -> launch.run());
        row.setOnLongClickListener(v -> { AppActionsMenu.show(this,app,row); return true; });
        return row;
    }
}
