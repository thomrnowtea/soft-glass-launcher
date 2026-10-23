package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;

final class TvPalette {
    final boolean light;
    final int background, surface, surfaceAlt, text, muted, line, accent, focus, onAccent;
    private TvPalette(boolean light) {
        this.light = light;
        background = c(light ? "#F2F2F4" : "#101113");
        surface = c(light ? "#FCFCFD" : "#242528");
        surfaceAlt = c(light ? "#E4E5E8" : "#303236");
        text = c(light ? "#191A1E" : "#F7F7F8");
        muted = c(light ? "#53555C" : "#C5C6CB");
        line = c(light ? "#30000000" : "#38FFFFFF");
        accent = c(light ? "#292B30" : "#EBECEF");
        focus = c(light ? "#25262B" : "#FFFFFF");
        onAccent = c(light ? "#FFFFFF" : "#151619");
    }
    static TvPalette of(Context context) {
        String selected = new TvPreferences(context).theme();
        boolean systemLight = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        return new TvPalette("light".equals(selected) || ("system".equals(selected) && systemLight));
    }
    private static int c(String value) { return Color.parseColor(value); }
}
