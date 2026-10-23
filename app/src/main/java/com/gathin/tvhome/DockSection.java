package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import java.util.function.Consumer;

final class DockSection {
    final LinearLayout root;
    private final LinearLayout apps;
    private final Context context;
    private final TvPalette palette;
    private String signature = "";
    private String focused = "";
    DockSection(Context context) {
        this.context = context; palette = TvPalette.of(context);
        root = TvUi.column(context);
        TextView label = TvUi.title(context, TvUi.label(context, "YOUR APPS", "TUS APPS"), 10, palette.muted);
        label.setLetterSpacing(.12f); label.setPadding(TvUi.dp(context, 12), 0, 0, TvUi.dp(context, 7)); root.addView(label);
        HorizontalScrollView scroll = new HorizontalScrollView(context); scroll.setHorizontalScrollBarEnabled(false);
        scroll.setBackground(TvUi.shape(context, palette.light ? 0xCFFFFFFF : 0xE019212E, palette.line, 24));
        scroll.setClipChildren(false); scroll.setClipToPadding(false);
        apps = TvUi.row(context); apps.setPadding(TvUi.dp(context, 12), TvUi.dp(context, 10), TvUi.dp(context, 4), TvUi.dp(context, 10));
        scroll.addView(apps); root.addView(scroll, TvUi.params(context, -1, 108, 0));
    }
    void show(List<AppAdapter.Entry> entries, Consumer<AppAdapter.Entry> launch, Runnable customize) {
        StringBuilder key = new StringBuilder(); for (AppAdapter.Entry app : entries) key.append(app.pkg).append(';');
        if (signature.equals(key.toString()) && apps.getChildCount() > 0) return;
        signature = key.toString(); apps.removeAllViews();
        int available = Math.round(context.getResources().getDisplayMetrics().widthPixels / context.getResources().getDisplayMetrics().density);
        int width = Math.min(164, Math.max(110, (available - 124) / Math.max(4, entries.size()) - 12));
        for (AppAdapter.Entry entry : entries) add(entry, width, launch, customize);
        TextView settings = TvUi.button(context, "+", palette, customize); settings.setContentDescription(TvUi.label(context, "Customize favorites", "Personalizar favoritas"));
        apps.addView(settings, TvUi.params(context, 44, 60, 8));
    }
    private void add(AppAdapter.Entry entry, int width, Consumer<AppAdapter.Entry> launch, Runnable customize) {
        FrameLayout frame = new FrameLayout(context); frame.setPadding(TvUi.dp(context, 3), TvUi.dp(context, 3), TvUi.dp(context, 3), TvUi.dp(context, 3));
        TvUi.surface(frame, palette, 18); TvUi.focus(frame, palette, 18, () -> focused = entry.pkg);
        ImageView banner = new ImageView(context); banner.setScaleType(ImageView.ScaleType.FIT_CENTER);
        banner.setBackground(TvUi.shape(context, entry.banner != null ? Color.BLACK : palette.surfaceAlt, 0, 15)); banner.setClipToOutline(true);
        banner.setImageDrawable(entry.banner != null ? entry.banner : entry.icon); frame.addView(banner, new FrameLayout.LayoutParams(-1, -1));
        frame.setContentDescription(entry.label); frame.setTag(entry.pkg);
        frame.setOnClickListener(view -> launch.accept(entry)); frame.setOnLongClickListener(view -> { customize.run(); return true; });
        apps.addView(frame, TvUi.params(context, width, 86, 10));
    }
    void focusFirst() { if (apps.getChildCount() > 0) apps.getChildAt(0).requestFocus(); }
    void restoreFocus() {
        for (int index = 0; index < apps.getChildCount(); index++) if (focused.equals(apps.getChildAt(index).getTag())) { apps.getChildAt(index).requestFocus(); return; }
        focusFirst();
    }
}
