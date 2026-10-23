package com.gathin.tvhome;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Uses the installed application's own artwork; focus never paints over a banner. */
final class AppTile extends LinearLayout {
    AppTile(Context context, AppAdapter.Entry app, int width, int height, boolean label, Runnable action) {
        super(context); setOrientation(VERTICAL); setClipChildren(false); setClipToPadding(false);
        TvPalette palette = TvPalette.of(context);
        FrameLayout banner = new FrameLayout(context);
        banner.setBackground(TvUi.shape(context,palette.surfaceAlt,0,8)); banner.setClipToOutline(true);
        ImageView image = new ImageView(context);
        Drawable artwork = app.banner != null ? app.banner : app.icon;
        image.setImageDrawable(artwork);
        TvAccessibility.decorative(image);
        image.setScaleType(app.banner != null ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.CENTER_INSIDE);
        if (app.banner == null) image.setPadding(dp(22),dp(16),dp(22),dp(16));
        banner.addView(image,new FrameLayout.LayoutParams(-1,-1));
        addView(banner,new LayoutParams(-1,dp(height)));
        if (label) {
            TextView title = TvUi.text(context,app.label,14,palette.muted); TvAccessibility.decorative(title);
            title.setSingleLine(); title.setEllipsize(TextUtils.TruncateAt.END);
            title.setGravity(Gravity.CENTER);
            title.setPadding(dp(4),dp(8),dp(4),dp(4)); addView(title,new LayoutParams(-1,-2));
        }
        setContentDescription(app.label); TvUi.focus(this,palette,8,1.015f,dp(2),null);
        android.view.View.OnFocusChangeListener feedback=getOnFocusChangeListener();
        setOnFocusChangeListener((view,focused) -> {
            feedback.onFocusChange(view,focused); banner.setForeground(getForeground()); setForeground(null);
        });
        setOnClickListener(v -> action.run()); setMinimumWidth(dp(width));
    }
    private int dp(float value) { return TvUi.dp(getContext(),value); }
}
