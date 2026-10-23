package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaMetadata;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

final class LivingHeader {
    final LinearLayout root;
    private final Activity activity;
    private final TextView clock,date,weather,track,artist;
    private final WeatherIcon weatherIcon;
    private final LinearLayout peek,notch;
    private final ImageView cover;
    private final SpotifyLevelsView levels;
    private final SpotifyNowPlaying spotify;
    LivingHeader(Activity activity,Runnable unused) {
        this.activity=activity; TvPalette p=TvPalette.of(activity); root=TvUi.row(activity);
        clock=TvUi.text(activity,"",45,p.text); clock.setLetterSpacing(-.045f); root.addView(clock,TvUi.params(activity,-2,-2,16));
        View divider=new View(activity); divider.setBackgroundColor(p.line); root.addView(divider,TvUi.params(activity,1,40,16));
        date=TvUi.text(activity,"",14,p.muted); date.setMaxLines(2); date.setEllipsize(TextUtils.TruncateAt.END);
        date.setLineSpacing(TvUi.dp(activity,4),1); root.addView(date,new LinearLayout.LayoutParams(0,-2,1));
        peek=TvUi.row(activity); peek.setPadding(TvUi.dp(activity,6),0,TvUi.dp(activity,9),0);
        weatherIcon=new WeatherIcon(activity,-1); peek.addView(weatherIcon,TvUi.params(activity,26,28,7));
        weather=TvUi.text(activity,"",14,p.text); weather.setMaxLines(2); weather.setEllipsize(TextUtils.TruncateAt.END);
        TvAccessibility.decorative(weather); peek.addView(weather,new LinearLayout.LayoutParams(0,-2,1));
        TvUi.focus(peek,p,10,null); peek.setOnClickListener(v -> activity.startActivity(new Intent(activity,
                WeatherLocation.configured(activity) ? WeatherActivity.class : WeatherLocationActivity.class)));
        peek.setMinimumHeight(TvUi.dp(activity,48)); root.addView(peek,TvUi.params(activity,106,-2,6));
        notch=TvUi.row(activity); notch.setPadding(TvUi.dp(activity,8),0,TvUi.dp(activity,8),0);
        notch.setBackground(GlassMaterial.surface(notch,p,20,false)); TvUi.focus(notch,p,20,1.015f,0,null);
        cover=new ImageView(activity); cover.setScaleType(ImageView.ScaleType.CENTER_CROP); TvAccessibility.decorative(cover);
        cover.setBackground(TvUi.shape(activity,p.surfaceAlt,0,5)); cover.setClipToOutline(true);
        notch.addView(cover,TvUi.params(activity,28,28,8));
        LinearLayout labels=TvUi.column(activity); track=TvUi.title(activity,"",14,p.text); artist=TvUi.text(activity,"",14,p.muted);
        track.setSingleLine(); track.setEllipsize(TextUtils.TruncateAt.END); artist.setSingleLine(); artist.setEllipsize(TextUtils.TruncateAt.END);
        labels.addView(track); TvUi.gap(labels,2); labels.addView(artist); notch.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        levels=new SpotifyLevelsView(activity,p); notch.addView(levels,TvUi.params(activity,20,20,0));
        notch.setMinimumHeight(TvUi.dp(activity,48)); notch.setOnClickListener(v -> openSpotify()); notch.setVisibility(View.GONE); root.addView(notch,TvUi.params(activity,180,-2,6));
        shortcut("bulb",TvUi.label(activity,"Lights","Luces"),() -> activity.startActivity(new Intent(activity,LightsActivity.class)));
        shortcut("store","Google Play Store",() -> StoreShortcut.open(activity));
        LinearLayout tv=TvUi.row(activity); tv.setGravity(Gravity.CENTER); tv.setBackground(GlassMaterial.surface(tv,p,24,false));
        tv.addView(new IconView(activity,"inputs",p.text),TvUi.params(activity,20,20,5)); tv.addView(TvUi.text(activity,"TV",16,p.text));
        TvUi.focus(tv,p,24,null); tv.setContentDescription("TV"); tv.setOnClickListener(v -> SystemLinks.tv(activity)); root.addView(tv,TvUi.params(activity,64,48,6));
        tv.setVisibility(SystemCapabilities.liveTv(activity)==null ? View.GONE : View.VISIBLE);
        shortcut("settings",activity.getString(R.string.settings),() -> activity.startActivity(new Intent(activity,QuickSettingsActivity.class)));
        spotify=new SpotifyNowPlaying(activity,this::music); tick(); weather(null);
    }
    private void shortcut(String icon,String label,Runnable action) { root.addView(TvUi.iconButton(activity,icon,label,true,action),TvUi.params(activity,48,48,6)); }
    void tick() {
        ZonedDateTime now=ZonedDateTime.now(); clock.setText(now.format(DateTimeFormatter.ofPattern("HH:mm")));
        Locale locale=activity.getResources().getConfiguration().getLocales().get(0);
        date.setText(now.format(DateTimeFormatter.ofPattern("EEEE",locale))+"\n"+now.format(DateTimeFormatter.ofPattern("d MMMM",locale)));
    }
    void weather(WeatherData data) {
        String city=WeatherLocation.load(activity).name; weatherIcon.code(data==null ? -1 : data.code);
        weather.setText((data==null ? "—" : Math.round(data.temperature)+"°")+"\n"+city);
        peek.setContentDescription(city+". "+(data==null ? activity.getString(R.string.hc_unavailable) : data.compact(activity)+". "+data.updated(activity)));
    }
    private void music(MediaMetadata data) {
        boolean focused=notch.hasFocus(); notch.setVisibility(data==null ? View.GONE : View.VISIBLE);
        levels.playing(data!=null);
        if (data==null) { cover.setImageDrawable(null); if (focused) peek.requestFocus(); return; }
        track.setText(data.getText(MediaMetadata.METADATA_KEY_TITLE)); artist.setText(data.getText(MediaMetadata.METADATA_KEY_ARTIST));
        android.graphics.Bitmap art=data.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art==null) art=data.getBitmap(MediaMetadata.METADATA_KEY_ART);
        cover.setImageBitmap(art); notch.setContentDescription("Spotify. "+track.getText()+". "+artist.getText());
    }
    private void openSpotify() {
        Intent intent=activity.getPackageManager().getLeanbackLaunchIntentForPackage("com.spotify.tv.android");
        if (intent==null) intent=activity.getPackageManager().getLaunchIntentForPackage("com.spotify.tv.android");
        if (intent!=null) SystemLinks.open(activity,intent);
    }
    void start() { spotify.start(); }
    void stop() { spotify.stop(); }
    void navigation(java.util.List<FocusNavigator.Row> rows) { FocusNavigator.add(rows,"header",root); }
}
