package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Function;

/** Consistent TV editor, including a deterministic Clear action for remote keyboards. */
final class TvTextDialog {
    static void show(Activity activity,String title,String initial,String hint,boolean secret,int limit,Function<String,String> save) {
        TvPalette palette=TvPalette.of(activity);
        int theme=palette.light ? R.style.Theme_LivingDialog_Light : R.style.Theme_LivingDialog_Dark;
        Dialog dialog=new Dialog(activity,theme); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout body=TvUi.column(dialog.getContext()); body.setPadding(dp(activity,24),dp(activity,22),dp(activity,24),dp(activity,20));
        body.setBackground(GlassMaterial.surface(body,palette,18,true));
        TextView heading=TvUi.title(activity,title,24,palette.text); TvAccessibility.heading(heading);
        body.addView(heading); TvUi.gap(body,18);
        TextView label=TvUi.text(activity,hint,14,palette.muted); body.addView(label); TvUi.gap(body,8);
        EditText input=editor(dialog.getContext(),palette,initial,hint,secret,limit);
        label.setLabelFor(input.getId()); body.addView(input,new LinearLayout.LayoutParams(-1,-2));
        TextView error=TvUi.text(activity,"",16,palette.text); error.setVisibility(View.GONE); TvAccessibility.status(error); body.addView(error);
        TvUi.gap(body,20);
        Runnable commit=() -> {
            String message=save.apply(input.getText().toString().trim());
            if (message!=null) { error.setText(message); error.setVisibility(View.VISIBLE); input.requestFocus(); return; }
            if (secret) input.setText(""); dialog.dismiss();
        };
        LinearLayout actions=TvUi.row(activity); body.addView(actions);
        TextView clear=TvUi.button(activity,TvUi.label(activity,"Clear","Borrar"),palette,() -> {
            input.setText(""); error.setVisibility(View.GONE); edit(input,false);
        });
        TextView cancel=TvUi.button(activity,activity.getString(android.R.string.cancel),palette,dialog::dismiss);
        TextView accept=TvUi.button(activity,activity.getString(R.string.hc_save),palette,commit);
        accept.setTextColor(palette.light ? 0xFFFFFFFF : palette.onAccent); accept.setBackground(GlassMaterial.emphasis(accept,palette,14,true));
        actions.addView(clear,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(0,-2,1); gap.setMarginStart(dp(activity,10));
        actions.addView(cancel,gap);
        LinearLayout.LayoutParams last=new LinearLayout.LayoutParams(0,-2,1); last.setMarginStart(dp(activity,10)); actions.addView(accept,last);
        wire(input,clear,cancel,accept);
        input.setOnEditorActionListener((view,action,event) -> { if (action!=EditorInfo.IME_ACTION_DONE) return false; commit.run(); return true; });
        dialog.setContentView(body);
        Window window=dialog.getWindow(); window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); window.setDimAmount(.18f);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.setOnShowListener(d -> edit(input,true));
        dialog.show();
        int screen=Math.round(activity.getResources().getDisplayMetrics().widthPixels/activity.getResources().getDisplayMetrics().density);
        window.setLayout(dp(activity,Math.min(440,screen-48)),WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
    }
    private static EditText editor(Context context,TvPalette palette,String initial,String hint,boolean secret,int limit) {
        EditText input=new EditText(context); input.setId(View.generateViewId()); input.setSingleLine();
        input.setDefaultFocusHighlightEnabled(false);
        input.setTextSize(18); input.setTypeface(context.getResources().getFont(R.font.manrope_regular));
        input.setTextColor(palette.text); input.setHintTextColor(palette.muted); input.setHint(hint);
        input.setHighlightColor(palette.light ? 0x55303035 : 0x667B7D84);
        input.setSaveEnabled(!secret); input.setSelectAllOnFocus(false);
        input.setInputType(InputType.TYPE_CLASS_TEXT|(secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        input.setPadding(dp(context,14),dp(context,12),dp(context,14),dp(context,12)); input.setMinHeight(dp(context,56));
        StateListDrawable background=new StateListDrawable();
        android.graphics.drawable.Drawable frost=GlassMaterial.surface(input,palette,10,false);
        background.addState(new int[]{android.R.attr.state_focused},new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{frost,GlassMaterial.focus(context,palette,10)}));
        background.addState(new int[]{},GlassMaterial.surface(input,palette,10,false)); input.setBackground(background);
        if (Build.VERSION.SDK_INT>=29) {
            GradientDrawable cursor=TvUi.shape(context,palette.text,0,0); cursor.setSize(dp(context,2),dp(context,24)); input.setTextCursorDrawable(cursor);
        }
        input.setText(initial);
        return input;
    }
    private static void edit(EditText input,boolean select) {
        input.requestFocus(); input.post(() -> {
            if (select) input.selectAll();
            InputMethodManager manager=(InputMethodManager)input.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (manager!=null) manager.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
        });
    }
    private static void wire(EditText input,View... actions) {
        input.setNextFocusDownId(actions[0].getId()); input.setNextFocusForwardId(actions[0].getId());
        for (int i=0;i<actions.length;i++) {
            View action=actions[i]; action.setNextFocusUpId(input.getId());
            action.setNextFocusLeftId(actions[Math.max(0,i-1)].getId());
            action.setNextFocusRightId(actions[Math.min(actions.length-1,i+1)].getId());
            action.setNextFocusForwardId(i+1<actions.length ? actions[i+1].getId() : input.getId());
        }
    }
    private static int dp(Context context,float value) { return TvUi.dp(context,value); }
}
