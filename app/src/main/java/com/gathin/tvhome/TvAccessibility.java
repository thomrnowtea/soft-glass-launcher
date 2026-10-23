package com.gathin.tvhome;

import android.os.Build;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Switch;

/** Semantics for custom native controls; labels never include credentials or decorative artwork. */
final class TvAccessibility {
    static void button(View view) {
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (Build.VERSION.SDK_INT>=28) view.setScreenReaderFocusable(true);
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host,info); info.setClassName(Button.class.getName());
            }
        });
    }
    static void heading(View view) {
        if (Build.VERSION.SDK_INT>=28) view.setAccessibilityHeading(true);
    }
    static void decorative(View view) { view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS); }
    static void status(View view) { view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); }
    static boolean exploring(Context context) {
        AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        return manager!=null && manager.isTouchExplorationEnabled();
    }
    static void group(ViewGroup view,String description) {
        view.setContentDescription(description); view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        if (Build.VERSION.SDK_INT>=28) view.setScreenReaderFocusable(true);
        for (int i=0;i<view.getChildCount();i++) decorative(view.getChildAt(i));
    }
    static void power(View view,String name,String state,String action,boolean checked,boolean known,boolean busy) {
        view.setContentDescription(name);
        if (Build.VERSION.SDK_INT>=30) view.setStateDescription(state);
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host,info);
                info.setClassName(Button.class.getName()); info.setCheckable(known); info.setChecked(checked);
                info.setClickable(!busy); info.setEnabled(!busy);
                if (Build.VERSION.SDK_INT<30) info.setText(state);
                if (!busy) info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK,action));
            }
        });
    }
}
