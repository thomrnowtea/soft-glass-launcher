package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Rect;
import android.widget.ScrollView;

/** Focus is positioned by the page coordinator; native auto-scroll must not race its animation. */
final class FocusScrollView extends ScrollView {
    FocusScrollView(Context context) { super(context); setFocusable(false); setOverScrollMode(OVER_SCROLL_NEVER); }
    @Override protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return TvAccessibility.exploring(getContext()) ? super.computeScrollDeltaToGetChildRectOnScreen(rect) : 0;
    }
}
