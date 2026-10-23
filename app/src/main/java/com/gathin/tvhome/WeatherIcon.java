package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Resolution-independent weather artwork; static drawing, no permanent animation. */
final class WeatherIcon extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int code;
    private final TvPalette palette;
    WeatherIcon(Context context, int code) { super(context); this.code = code; palette = TvPalette.of(context); }
    void code(int value) { code = value; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save(); canvas.scale(getWidth() / 100f, getHeight() / 100f);
        if (code < 0) { canvas.restore(); return; }
        if (code <= 2) sun(canvas);
        if (code > 0) cloud(canvas);
        if (code >= 51) rain(canvas);
        canvas.restore();
    }
    private void sun(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.rgb(247, 187, 74));
        canvas.drawCircle(code == 0 ? 50 : 35, code == 0 ? 48 : 35, 21, paint);
        paint.setStrokeWidth(3); paint.setStrokeCap(Paint.Cap.ROUND);
        for (int index = 0; index < 8; index++) {
            canvas.save(); canvas.rotate(index * 45, code == 0 ? 50 : 35, code == 0 ? 48 : 35);
            float x = code == 0 ? 50 : 35, y = code == 0 ? 48 : 35;
            canvas.drawLine(x, y - 28, x, y - 33, paint); canvas.restore();
        }
    }
    private void cloud(Canvas canvas) {
        paint.setColor(palette.light ? Color.rgb(178, 192, 210) : Color.rgb(206, 218, 234));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(20, 49, 88, 74), 13, 13, paint);
        canvas.drawCircle(43, 47, 20, paint); canvas.drawCircle(66, 51, 16, paint);
    }
    private void rain(Canvas canvas) {
        paint.setStrokeWidth(4); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.rgb(78, 155, 245));
        for (int index = 0; index < 3; index++) canvas.drawLine(35 + index * 17, 83, 30 + index * 17, 92, paint);
    }
}
