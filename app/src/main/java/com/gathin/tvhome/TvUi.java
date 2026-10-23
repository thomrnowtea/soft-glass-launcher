package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.ContextThemeWrapper;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;

/** Shared native surfaces and focus feedback. Animations change only render properties. */
final class TvUi {
    static final PathInterpolator EASE_OUT = new PathInterpolator(.23f, 1f, .32f, 1f);
    static final PathInterpolator EASE_DRAWER = new PathInterpolator(.32f,.72f,0,1);
    private TvUi() { }
    static int dp(Context context, float value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    static TextView text(Context context, CharSequence value, int size, int color) {
        TextView text = new TextView(context);
        text.setText(value); text.setTextSize(size); text.setTextColor(color);
        text.setTypeface(context.getResources().getFont(R.font.manrope_regular));
        text.setIncludeFontPadding(false);
        return text;
    }
    static TextView title(Context context, CharSequence value, int size, int color) {
        TextView text = text(context, value, size, color);
        text.setTypeface(context.getResources().getFont(R.font.manrope_medium), Typeface.NORMAL);
        text.setLetterSpacing(size >= 28 ? -.025f : 0f);
        return text;
    }
    static LinearLayout column(Context context) {
        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false); column.setClipToPadding(false);
        return column;
    }
    static LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClipChildren(false); row.setClipToPadding(false);
        return row;
    }
    static GradientDrawable shape(Context context, int color, int stroke, float radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color); shape.setCornerRadius(dp(context, radius));
        if (stroke != Color.TRANSPARENT) shape.setStroke(dp(context, 1), stroke);
        return shape;
    }
    static void surface(View view, TvPalette palette, float radius) {
        view.setBackground(GlassMaterial.surface(view,palette,radius,false));
        view.setElevation(dp(view.getContext(),2));
    }
    static void focus(View view, TvPalette palette, float radius, Runnable selected) {
        focus(view,palette,radius,1.02f,dp(view.getContext(),2),selected);
    }
    static void focus(View view,TvPalette palette,float radius,float scale,float lift,Runnable selected) {
        view.setFocusable(true); view.setClickable(true); view.setId(View.generateViewId());
        view.setDefaultFocusHighlightEnabled(false);
        TvAccessibility.button(view);
        TvMotion.Focus motion=new TvMotion.Focus(view,scale,lift);
        android.graphics.drawable.Drawable contour=GlassMaterial.focus(view.getContext(),palette,radius);
        view.setOnFocusChangeListener((item, focused) -> {
            item.setForeground(focused ? contour : null);
            motion.focus(focused);
            if (focused && selected != null) selected.run();
        });
        view.setOnKeyListener((item,key,event) -> motion.key(event));
    }
    static TextView button(Context context, String label, TvPalette palette, Runnable action) {
        return button(context,label,palette,action,18);
    }
    static TextView button(Context context,String label,TvPalette palette,Runnable action,float radius) {
        TextView button = title(context, label, 16, palette.text);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(context, 18), 0, dp(context, 18), 0);
        button.setMinHeight(dp(context, 48)); button.setMinWidth(dp(context,48));
        surface(button, palette, radius); focus(button, palette, radius, null);
        button.setOnClickListener(view -> action.run());
        return button;
    }
    static TextView plain(Context context, String label, Runnable action) {
        TvPalette palette = TvPalette.of(context);
        TextView view = title(context, label, 16, palette.text);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10));
        view.setMinHeight(dp(context,48)); view.setMinWidth(dp(context,48));
        focus(view, palette, 8, null); view.setOnClickListener(v -> action.run());
        return view;
    }
    static void buttonIcons(TextView button,int leading,int trailing,int color) {
        button.setCompoundDrawablePadding(dp(button.getContext(),8));
        button.setCompoundDrawablesRelative(buttonIcon(button.getContext(),leading,color),null,
                buttonIcon(button.getContext(),trailing,color),null);
    }
    private static android.graphics.drawable.Drawable buttonIcon(Context context,int resource,int color) {
        if (resource==0) return null;
        android.graphics.drawable.Drawable icon=context.getDrawable(resource).mutate();
        icon.setTint(color); icon.setBounds(0,0,dp(context,20),dp(context,20)); return icon;
    }
    static FrameLayout iconButton(Context context, String icon, String label, boolean material, Runnable action) {
        TvPalette palette = TvPalette.of(context);
        FrameLayout frame = new FrameLayout(context);
        frame.setMinimumWidth(dp(context,48)); frame.setMinimumHeight(dp(context,48));
        if (material) frame.setBackground(GlassMaterial.surface(frame,palette,24,false));
        IconView drawing = new IconView(context, icon, palette.text);
        TvAccessibility.decorative(drawing);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(context, 22), dp(context, 22), Gravity.CENTER);
        frame.addView(drawing, params); frame.setContentDescription(label);
        focus(frame, palette, 24,1.035f,dp(context,3),null); frame.setOnClickListener(v -> action.run()); return frame;
    }
    static void line(LinearLayout parent) {
        View line = new View(parent.getContext()); line.setBackgroundColor(TvPalette.of(parent.getContext()).line);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(parent.getContext(), .5f)));
    }
    static LinearLayout.LayoutParams params(Context context, int width, int height, int margin) {
        LinearLayout.LayoutParams result = new LinearLayout.LayoutParams(width < 0 ? width : dp(context, width), height < 0 ? height : dp(context, height));
        result.setMarginEnd(dp(context, margin)); return result;
    }
    static void gap(LinearLayout parent, int height) {
        parent.addView(new View(parent.getContext()), params(parent.getContext(), 1, height, 0));
    }
    static void appear(View view) {
        if (!TvMotion.enabled(view.getContext())) return;
        view.setAlpha(0f); view.animate().alpha(1f).setDuration(180).setInterpolator(EASE_OUT).start();
    }
    static String label(Context context, String english, String spanish) {
        return context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("es") ? spanish : english;
    }
    static AlertDialog.Builder dialog(Context context) {
        int theme = TvPalette.of(context).light ? R.style.Theme_LivingDialog_Light : R.style.Theme_LivingDialog_Dark;
        return new AlertDialog.Builder(new ContextThemeWrapper(context, theme)) {
            @Override public AlertDialog create() {
                AlertDialog dialog=super.create();
                dialog.setOnShowListener(ignored -> {
                    if (dialog.getWindow()!=null) {
                        dialog.getWindow().setBackgroundDrawable(GlassMaterial.surface(dialog.getWindow().getDecorView(),TvPalette.of(context),18,true));
                        dialog.getWindow().getDecorView().setClipToOutline(true);
                    }
                });
                return dialog;
            }
        };
    }
}
