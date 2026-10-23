package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

/** Shared frosted materials. Static gradients keep focus and sheet motion on the render thread. */
final class GlassMaterial {
    static GradientDrawable card(android.view.View view,TvPalette palette,float radius) {
        return frost(view,card(view.getContext(),palette,radius),palette,radius);
    }
    static GradientDrawable surface(android.view.View view,TvPalette palette,float radius,boolean panel) {
        return frost(view,surface(view.getContext(),palette,radius,panel),palette,radius);
    }
    static GradientDrawable emphasis(android.view.View view,TvPalette palette,float radius,boolean selected) {
        return frost(view,emphasis(view.getContext(),palette,radius,selected),palette,radius);
    }
    private static GradientDrawable frost(android.view.View view,GradientDrawable tint,TvPalette palette,float radius) {
        return finish(view.getContext(),new FrostedDrawable(view,tint.getColors(),radius,palette),palette,radius);
    }
    static GradientDrawable card(Context context,TvPalette palette,float radius) {
        return material(context,palette,radius,palette.light
                ? new int[]{0x80FFFFFF,0x4DEEF0F3,0x66E3E6EC}
                : new int[]{0x6133363B,0x36272A30,0x5224272D});
    }
    static GradientDrawable surface(Context context,TvPalette palette,float radius,boolean panel) {
        int[] colors;
        // Sheets use thicker frost: Home remains visible around them without text bleeding through.
        if (panel) colors=palette.light
                ? new int[]{0xA6F9FAFC,0x85EDEFF3,0x99E2E5EB}
                : new int[]{0x8533363B,0x662B2F36,0x8520232A};
        else colors=palette.light
                ? new int[]{0x85E4E6EB,0x52CDD1D9,0x66D5D9E0}
                : new int[]{0x7033363B,0x382D3036,0x662B2F36};
        return material(context,palette,radius,colors);
    }
    static GradientDrawable emphasis(Context context,TvPalette palette,float radius,boolean selected) {
        if (!selected) return surface(context,palette,radius,false);
        return material(context,palette,radius,palette.light
                ? new int[]{0xD94F545D,0xCC3F454F,0xD9333943}
                : new int[]{0xD9FFFFFF,0xC7E5E8EE,0xD9D3D8E1});
    }
    private static GradientDrawable material(Context context,TvPalette palette,float radius,int[] colors) {
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);
        return finish(context,shape,palette,radius);
    }
    private static GradientDrawable finish(Context context,GradientDrawable shape,TvPalette palette,float radius) {
        shape.setCornerRadius(TvUi.dp(context,radius));
        shape.setStroke(Math.max(1,TvUi.dp(context,.5f)),palette.light ? 0x9EFFFFFF : 0x2EFFFFFF);
        return shape;
    }
    static Drawable focus(Context context,TvPalette palette,float radius) {
        GradientDrawable halo=TvUi.shape(context,Color.TRANSPARENT,0,radius);
        halo.setStroke(TvUi.dp(context,4),palette.light ? 0xFFFFFFFF : 0xFF101113);
        GradientDrawable shape=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                palette.light ? new int[]{0x08FFFFFF,Color.TRANSPARENT} : new int[]{0x12FFFFFF,0x02FFFFFF});
        shape.setCornerRadius(TvUi.dp(context,radius));
        shape.setStroke(TvUi.dp(context,2),palette.focus); return new LayerDrawable(new Drawable[]{halo,shape});
    }
}
