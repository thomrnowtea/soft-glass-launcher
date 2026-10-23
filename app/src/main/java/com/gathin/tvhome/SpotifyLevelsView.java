package com.gathin.tvhome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/** Native vector playback indicator; no font glyphs, audio capture or per-frame layout work. */
final class SpotifyLevelsView extends ImageView {
    private final AnimatedVectorDrawable animated;
    private final Drawable still;
    private final Rect visibleBounds=new Rect();
    private boolean playing;
    private final ViewTreeObserver.OnScrollChangedListener scrollChanged=this::update;

    SpotifyLevelsView(Context context,TvPalette palette) {
        super(context);
        animated=(AnimatedVectorDrawable)context.getDrawable(R.drawable.lr_spotify_levels_animated).mutate();
        still=context.getDrawable(R.drawable.lr_spotify_levels).mutate();
        setImageTintList(ColorStateList.valueOf(palette.light ? 0xFF16783B : 0xFF1ED760));
        setScaleType(ScaleType.FIT_CENTER); setImageDrawable(still); TvAccessibility.decorative(this);
    }
    void playing(boolean value) { playing=value; update(); }
    private void update() {
        // ImageView's constructor can dispatch a visibility callback before our fields exist.
        if (animated==null) return;
        if (shouldAnimate()) {
            if (getDrawable()!=animated) setImageDrawable(animated);
            if (!animated.isRunning()) animated.start();
        } else {
            if (animated.isRunning()) animated.stop();
            if (getDrawable()!=still) setImageDrawable(still);
        }
    }
    private boolean shouldAnimate() { return playing && onScreen() && TvMotion.enabled(getContext()); }
    private boolean onScreen() {
        return isAttachedToWindow() && isShown() && hasWindowFocus()
                && getWindowVisibility()==View.VISIBLE && getGlobalVisibleRect(visibleBounds);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow(); getViewTreeObserver().addOnScrollChangedListener(scrollChanged); post(this::update);
    }
    @Override protected void onDetachedFromWindow() {
        getViewTreeObserver().removeOnScrollChangedListener(scrollChanged); animated.stop(); super.onDetachedFromWindow();
    }
    @Override public void onVisibilityAggregated(boolean visible) { super.onVisibilityAggregated(visible); update(); }
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight) {
        super.onSizeChanged(width,height,oldWidth,oldHeight); update();
    }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); update(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); update(); }
}
