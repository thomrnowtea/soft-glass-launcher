package com.gathin.tvhome;
import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;
import java.util.HashMap;
import java.util.Map;

/** Theme-tinted vector resources shared by all remote shortcuts. */
final class IconView extends ImageView {
    private static final Map<String,Integer> GLYPHS = new HashMap<>();
    static {
        GLYPHS.put("bulb", R.drawable.lr_bulb); GLYPHS.put("store", R.drawable.lr_store);
        GLYPHS.put("inputs", R.drawable.lr_inputs); GLYPHS.put("settings", R.drawable.lr_settings);
        GLYPHS.put("grid", R.drawable.lr_grid); GLYPHS.put("close", R.drawable.lr_close);
        GLYPHS.put("pencil", R.drawable.lr_pencil); GLYPHS.put("wifi", R.drawable.lr_wifi);
        GLYPHS.put("hdmi", R.drawable.lr_hdmi); GLYPHS.put("play", R.drawable.lr_play);
        GLYPHS.put("plus", R.drawable.lr_plus); GLYPHS.put("up", R.drawable.lr_up);
        GLYPHS.put("down", R.drawable.lr_down); GLYPHS.put("left", R.drawable.lr_left);
        GLYPHS.put("right", R.drawable.lr_right);
        GLYPHS.put("sound",R.drawable.lr_sound); GLYPHS.put("search",R.drawable.lr_search); GLYPHS.put("mic",R.drawable.lr_mic);
        GLYPHS.put("picture",R.drawable.ic_picture); GLYPHS.put("accessibility",R.drawable.lr_accessibility);
    }
    IconView(Context context, String icon, int color) {
        super(context);
        Integer resource = GLYPHS.get(icon); setImageResource(resource == null ? R.drawable.lr_down : resource);
        setImageTintList(ColorStateList.valueOf(color)); setScaleType(ScaleType.FIT_CENTER);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
}
