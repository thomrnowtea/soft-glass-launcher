package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/** Static gradients over real artwork; no live blur or per-frame allocations. */
final class BackdropShade extends View {
    private final Paint horizontal = new Paint(), vertical = new Paint();
    private final boolean light;
    private final boolean widgets;
    BackdropShade(Context context) { this(context,false); }
    BackdropShade(Context context,boolean widgets) { super(context); light = TvPalette.of(context).light; this.widgets=widgets; }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (widgets) { widgetGradient(w,h); return; }
        int[] across = light ? new int[]{0xFAF2F2F4,0xF2F2F2F4,0x10F2F2F4} : new int[]{0xF7101113,0xED101113,0x38101113};
        horizontal.setShader(new LinearGradient(0, 0, w, 0, across, new float[]{0,.56f,.98f}, Shader.TileMode.CLAMP));
        int[] down = light ? new int[]{0xA6F2F2F4,0x0DF2F2F4,0x0DF2F2F4,0xF5F2F2F4,0xFFF2F2F4}
                : new int[]{0x8C101113,0x40101113,0x1F101113,0xE6101113,0xFF101113};
        vertical.setShader(new LinearGradient(0, 0, 0, h, down, new float[]{0,.24f,.58f,.86f,1}, Shader.TileMode.CLAMP));
    }
    private void widgetGradient(int w,int h) {
        int[] across={tone(light ? 0xBF : 0xB8),tone(light ? 0xB3 : 0xA3),tone(light ? 0xA8 : 0x94)};
        horizontal.setShader(new LinearGradient(0,0,w,0,across,new float[]{0,.5f,1},Shader.TileMode.CLAMP));
        int[] down={tone(light ? 0x88 : 0x66),tone(0x99),tone(0xC2),tone(0xE6),tone(0xF2)};
        vertical.setShader(new LinearGradient(0,0,0,h,down,new float[]{0,.25f,.62f,.9f,1},Shader.TileMode.CLAMP));
    }
    private int tone(int alpha) { return alpha<<24 | (light ? 0x00F2F2F4 : 0x00101113); }
    @Override protected void onDraw(Canvas canvas) { canvas.drawRect(0,0,getWidth(),getHeight(),horizontal); canvas.drawRect(0,0,getWidth(),getHeight(),vertical); }
}
