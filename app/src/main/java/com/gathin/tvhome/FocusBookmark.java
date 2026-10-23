package com.gathin.tvhome;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Semantic focus survives replacing views; generated IDs do not survive a palette rebuild. */
final class FocusBookmark {
    private final String key;
    private FocusBookmark(String key) { this.key=key; }
    static FocusBookmark capture(View focus,View root) {
        return new FocusBookmark(LivingHome.within(focus,root) ? key(focus) : "");
    }
    void restore(View root) {
        if (key.isEmpty()) return;
        View match=find(root);
        if (match!=null) match.requestFocus();
    }
    private View find(View view) {
        if (view.getVisibility()!=View.VISIBLE || !view.isEnabled()) return null;
        if (view.isFocusable() && view.getVisibility()==View.VISIBLE && key.equals(key(view))) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for (int i=0;i<group.getChildCount();i++) {
                View match=find(group.getChildAt(i)); if (match!=null) return match;
            }
        }
        return null;
    }
    private static String key(View view) {
        if (view.getContentDescription()!=null) return view.getContentDescription().toString();
        return view instanceof TextView ? ((TextView)view).getText().toString() : "";
    }
}
