package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded artwork cache. Only public TMDB / Cinemeta image hosts are accepted. */
final class ArtworkCache {
    private static final LruCache<String,Bitmap> MEMORY = new LruCache<String,Bitmap>(10*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value) { return value.getAllocationByteCount(); }
    };
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final File directory;
    private boolean closed;
    ArtworkCache(Context context) { directory = new File(context.getCacheDir(),"movie_artwork"); directory.mkdirs(); }
    void load(ImageView view,String path,boolean hero) {
        if (closed) return;
        String url = endpoint(path,hero);
        view.setTag(url);
        if (url.isEmpty()) { view.setImageDrawable(null); if (hero) FrostedBackdrop.artwork(null); return; }
        Bitmap cached = MEMORY.get(url);
        if (cached != null) { display(view,cached,url,hero); return; }
        workers.execute(() -> {
            try {
                Bitmap bitmap = download(url);
                main.post(() -> { if (!closed && url.equals(view.getTag())) display(view,bitmap,url,hero); });
            } catch (IOException ignored) { }
        });
    }
    private void display(ImageView view,Bitmap image,String key,boolean hero) {
        if (hero) FrostedBackdrop.artwork(image);
        android.graphics.drawable.Drawable previous=view.getDrawable();
        if (previous instanceof android.graphics.drawable.TransitionDrawable) {
            android.graphics.drawable.TransitionDrawable transition=(android.graphics.drawable.TransitionDrawable)previous;
            previous=transition.getDrawable(transition.getNumberOfLayers()-1);
        }
        android.graphics.drawable.BitmapDrawable next=new android.graphics.drawable.BitmapDrawable(view.getResources(),image);
        if (previous==null || !TvMotion.enabled(view.getContext())) { view.setImageDrawable(next); return; }
        if (previous instanceof android.graphics.drawable.BitmapDrawable && ((android.graphics.drawable.BitmapDrawable)previous).getBitmap()==image) return;
        android.graphics.drawable.TransitionDrawable transition=new android.graphics.drawable.TransitionDrawable(new android.graphics.drawable.Drawable[]{previous,next});
        transition.setCrossFadeEnabled(true); view.setImageDrawable(transition); transition.startTransition(220);
        main.postDelayed(() -> { if (!closed && key.equals(view.getTag()) && view.getDrawable()==transition) view.setImageDrawable(next); },220);
    }
    private String endpoint(String path,boolean hero) {
        if (path.matches("/[A-Za-z0-9._/-]+") && !path.contains(".."))
            return "https://image.tmdb.org/t/p/" + (hero ? "w1280" : "w342") + path;
        try { URL url = new URL(path); return allowed(url) ? url.toString() : ""; }
        catch (Exception error) { return ""; }
    }
    private static boolean allowed(URL url) {
        if (!url.getProtocol().equals("https") || url.getUserInfo()!=null || (url.getPort()!=-1 && url.getPort()!=443)) return false;
        String host = url.getHost();
        return host.equals("image.tmdb.org") || host.equals("images.metahub.space") || host.equals("live.metahub.space");
    }
    private Bitmap download(String url) throws IOException {
        Bitmap memory = MEMORY.get(url); if (memory != null) return memory;
        File file = new File(directory,hash(url)); byte[] bytes;
        if (file.isFile() && file.length()<=4*1024*1024) bytes = Files.readAllBytes(file.toPath());
        else { bytes = fetch(new URL(url),0); Files.write(file.toPath(),bytes); trim(); }
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
        if (bounds.outWidth<=0 || (long)bounds.outWidth*bounds.outHeight>12_000_000) { file.delete(); throw new IOException("Invalid artwork"); }
        BitmapFactory.Options sample = new BitmapFactory.Options(); sample.inSampleSize = 1;
        while ((long)(bounds.outWidth/sample.inSampleSize)*(bounds.outHeight/sample.inSampleSize)>2_500_000) sample.inSampleSize*=2;
        Bitmap image = BitmapFactory.decodeByteArray(bytes,0,bytes.length,sample);
        if (image==null) { file.delete(); throw new IOException("Invalid artwork"); }
        MEMORY.put(url,image); return image;
    }
    private byte[] fetch(URL url,int redirects) throws IOException {
        if (!allowed(url) || redirects>2) throw new IOException("Unsupported artwork host");
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(6000); connection.setReadTimeout(6000);
        try {
            int status = connection.getResponseCode();
            if (status>=300 && status<400) {
                String location = connection.getHeaderField("Location");
                if (location==null) throw new IOException("Artwork unavailable");
                return fetch(new URL(url,location),redirects+1);
            }
            if (status!=200) throw new IOException("Artwork unavailable");
            try (InputStream input = connection.getInputStream()) { return NetJson.read(input,4*1024*1024); }
        } finally { connection.disconnect(); }
    }
    private static String hash(String url) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(url.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(); for (byte value : bytes) result.append(String.format(java.util.Locale.ROOT,"%02x",value));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    private synchronized void trim() {
        File[] files = directory.listFiles(); if (files==null) return;
        Arrays.sort(files,Comparator.comparingLong(File::lastModified));
        long total=0; for (File file : files) total+=file.length(); int count=files.length;
        for (File file : files) { if (total<=24*1024*1024 && count<=40) break; long size=file.length(); if (file.delete()) { total-=size; count--; } }
    }
    void close() { closed=true; main.removeCallbacksAndMessages(null); workers.shutdownNow(); }
}
