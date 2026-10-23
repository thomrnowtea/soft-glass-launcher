package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, event-driven frost textures for Android 11. No screenshot permission or live blur loop. */
final class FrostedBackdrop {
    static final class Texture {
        final Bitmap dark,light;
        final int width,height,left,top;
        Texture(Bitmap bitmap,int width,int height,int left,int top) {
            dark=tone(bitmap,false); light=tone(bitmap,true);
            bitmap.recycle(); this.width=width; this.height=height; this.left=left; this.top=top;
        }
        Bitmap bitmap(boolean lightTheme) { return lightTheme ? light : dark; }
    }
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static final Map<View,Boolean> MATERIALS=new WeakHashMap<>();
    private static final Map<Activity,Texture> SHEETS=new WeakHashMap<>();
    private static WeakReference<View> home=new WeakReference<>(null);
    private static Texture ambient;
    private static Bitmap artwork;
    private static volatile int artGeneration;

    static void home(View root) { home=new WeakReference<>(root); }
    static void track(View view) { MATERIALS.put(view,true); }
    static Texture texture(View view) {
        Texture sheet=SHEETS.get(activity(view.getContext()));
        return sheet==null ? ambient : sheet;
    }
    static void artwork(Bitmap image) {
        if (image==artwork) return;
        artwork=image; int request=++artGeneration;
        if (image==null) { ambient=null; invalidate(); return; }
        View root=home.get(); if (root==null) return;
        int width=root.getResources().getDisplayMetrics().widthPixels;
        int height=root.getResources().getDisplayMetrics().heightPixels;
        WORK.execute(() -> {
            if (request!=artGeneration) return;
            Bitmap small=Bitmap.createScaledBitmap(image,96,54,true);
            // Never mutate the artwork cache's bitmap, including an already-small source.
            Bitmap copy=small.copy(Bitmap.Config.ARGB_8888,true);
            if (small!=image) small.recycle();
            blur(copy);
            Texture texture=new Texture(copy,width,height,0,0);
            MAIN.post(() -> {
                if (request!=artGeneration) { texture.dark.recycle(); texture.light.recycle(); return; }
                ambient=texture; invalidate();
            });
        });
    }
    static void prepare(Activity panel) {
        View source=home.get();
        if (source==null || source.getWidth()==0 || source.getHeight()==0) return;
        int width=source.getWidth(),height=source.getHeight();
        Bitmap bitmap=Bitmap.createBitmap(160,Math.max(1,Math.round(160f*height/width)),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap); canvas.scale(bitmap.getWidth()/(float)width,bitmap.getHeight()/(float)height);
        source.draw(canvas);
        int[] location=new int[2]; source.getLocationOnScreen(location);
        WeakReference<Activity> owner=new WeakReference<>(panel);
        WORK.execute(() -> {
            blur(bitmap);
            Texture texture=new Texture(bitmap,width,height,location[0],location[1]);
            MAIN.post(() -> {
                Activity target=owner.get();
                if (target==null || target.isDestroyed() || target.isFinishing()) { texture.dark.recycle(); texture.light.recycle(); return; }
                SHEETS.put(target,texture); invalidate();
            });
        });
    }
    private static Activity activity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity)context;
            Context next=((ContextWrapper)context).getBaseContext();
            if (next==context) break;
            context=next;
        }
        return null;
    }
    private static void invalidate() {
        for (View view:new ArrayList<>(MATERIALS.keySet())) if (view.isAttachedToWindow()) view.invalidate();
    }
    private static void blur(Bitmap bitmap) {
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        int[] pixels=new int[w*h]; bitmap.getPixels(pixels,0,w,0,0,w,h);
        for (int pass=0;pass<3;pass++) {
            pixels=HeroArt.boxBlur(pixels,w,h,3,true);
            pixels=HeroArt.boxBlur(pixels,w,h,3,false);
        }
        bitmap.setPixels(pixels,0,w,0,0,w,h);
    }
    /** Bound luminance in the texture, so the glass tint itself can stay genuinely translucent. */
    private static Bitmap tone(Bitmap source,boolean light) {
        int w=source.getWidth(),h=source.getHeight(); int[] pixels=new int[w*h];
        source.getPixels(pixels,0,w,0,0,w,h);
        for (int i=0;i<pixels.length;i++) {
            int p=pixels[i];
            int r=channel((p>>16)&255,light),g=channel((p>>8)&255,light),b=channel(p&255,light);
            pixels[i]=0xFF000000 | r<<16 | g<<8 | b;
        }
        return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
    }
    private static int channel(int value,boolean light) {
        return light ? 214+value*41/255 : 14+value*66/255;
    }
}
