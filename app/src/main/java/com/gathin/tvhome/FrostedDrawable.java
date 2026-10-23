package com.gathin.tvhome;

import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import java.lang.ref.WeakReference;

/** Cached blur below the shared tint, clipped with a GPU-friendly rounded rectangle. */
final class FrostedDrawable extends GradientDrawable {
    private final WeakReference<View> host;
    private final Paint frost=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint sheen=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix transform=new Matrix();
    private final int[] location=new int[2];
    private final float radius;
    private final boolean light;
    private final int fallback;
    private FrostedBackdrop.Texture current;
    private BitmapShader shader;
    FrostedDrawable(View view,int[] colors,float radius,TvPalette palette) {
        super(Orientation.TL_BR,colors);
        host=new WeakReference<>(view); this.radius=TvUi.dp(view.getContext(),radius);
        light=palette.light; fallback=palette.surface;
        FrostedBackdrop.track(view);
    }
    @Override protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        sheen.setShader(new LinearGradient(bounds.left,bounds.top,bounds.right,bounds.bottom,
                new int[]{light ? 0x40FFFFFF : 0x0EFFFFFF,0x00FFFFFF,light ? 0x0AFFFFFF : 0x05FFFFFF},
                new float[]{0,.45f,1},Shader.TileMode.CLAMP));
    }
    @Override public void draw(Canvas canvas) {
        View view=host.get();
        FrostedBackdrop.Texture texture=view==null ? null : FrostedBackdrop.texture(view);
        Rect bounds=getBounds();
        if (texture!=null) {
            if (texture!=current) {
                current=texture; shader=new BitmapShader(texture.bitmap(light),Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
                frost.setShader(shader);
            }
            view.getLocationOnScreen(location);
            transform.setScale(texture.width/(float)texture.bitmap(light).getWidth(),texture.height/(float)texture.bitmap(light).getHeight());
            transform.postTranslate(texture.left-location[0],texture.top-location[1]); shader.setLocalMatrix(transform);
        } else {
            frost.setShader(null); frost.setColor(fallback); current=null;
        }
        canvas.drawRoundRect(bounds.left,bounds.top,bounds.right,bounds.bottom,radius,radius,frost);
        super.draw(canvas);
        canvas.drawRoundRect(bounds.left,bounds.top,bounds.right,bounds.bottom,radius,radius,sheen);
    }
}
