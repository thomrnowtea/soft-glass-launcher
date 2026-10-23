package com.gathin.tvhome;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small local bilingual grammar. Requests and search history are never stored by this launcher. */
final class SearchRouter {
    private static final int FLAGS=Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE;
    private static final String[] APPS={"store","stremio","stremio","spotify"};
    private static final Pattern[] RULES={
        rule("^(?:download|install|get|descargar|descarg[aá](?:me)?|instalar|instal[aá](?:me)?|bajar|baj[aá])\\s+(?:(?:(?:la|una|the|an?)\\s+)?app\\s+(?:(?:llamada|called)\\s+)?)?(.+)$"),
        rule("^(?:play|pon[eé](?:me)?|reproducir|reproduce|reproduc[ií])\\s+(?:(?:la|una|the|a)\\s+)?(?:pel[ií]cula|peli|serie|movie|film|show)\\s+(.+)$"),
        rule("^(?:watch|see|ver|mirar|mir[aá])\\s+(?:(?:(?:la|una|the|a)\\s+)?(?:pel[ií]cula|peli|serie|movie|film|show)\\s+)?(.+)$"),
        rule("^(?:play|reproducir|reproduce|reproduc[ií]|escuchar|escuch[aá](?:me)?|listen(?:\\s+to)?|pon[eé](?:me)?|ponme)\\s+(?:(?:(?:la|una|the|a)\\s+)?(?:canci[oó]n|tema|song|track|m[uú]sica)\\s+)?(.+)$")
    };
    private static Pattern rule(String expression) { return Pattern.compile(expression,FLAGS); }
    private static String replace(String text,String expression,String replacement) { return rule(expression).matcher(text).replaceFirst(replacement); }
    static void open(Activity activity,String command) {
        String request=command.trim(); if (request.isEmpty()) return;
        if (rule("^[¡¿\\s]*(?:hola|buenas|hello|hi|hey|help|ayuda)[!?.\\s]*$").matcher(request).matches()) {
            Toast.makeText(activity,TvUi.label(activity,"Search for a movie, song or app.","¿Qué querés ver, escuchar o instalar?"),Toast.LENGTH_LONG).show(); return;
        }
        request=clean(request);
        Matcher destination=rule("^(.+?)\\s+(?:en|on|in)\\s+(stremio|spotify|play store|google play)[.!?]*$").matcher(request);
        String app="",body=request;
        if (destination.matches()) { body=destination.group(1); app=destination.group(2).toLowerCase(Locale.ROOT); }
        for (int i=0;i<RULES.length;i++) {
            Matcher match=RULES[i].matcher(body);
            if (match.matches()) { launch(activity,app.isEmpty() ? APPS[i] : app,match.group(1).replaceAll("[.!?]+$","").trim()); return; }
        }
        launch(activity,app.isEmpty() ? "stremio" : app,replace(body,"^(?:buscar|busc[aá](?:me)?|b[uú]scame|search(?:\\s+for)?|look for|find)\\s+",""));
    }
    private static String clean(String request) {
        request=replace(request,"^(?:hola|hello|hey)[,!]\\s*","");
        request=replace(request,"^(?:por favor|please)[,\\s]+","");
        request=replace(request,"^(?:quiero|quisiera|necesito|me gustar[ií]a|puedes|pod[eé]s|i want to|i need to|i(?:['’]d| would) like to|can you|could you)\\s+","");
        request=replace(request,"^(?:por favor|please)[,\\s]+","");
        request=replace(request,"[,\\s]+(?:por favor|please)[.!?]*$","");
        return request;
    }
    private static void launch(Activity activity,String app,String query) {
        if (query.isEmpty()) return;
        if (query.length()>240) query=query.substring(0,240);
        if (app.contains("store") || app.equals("google play")) {
            Intent store=new Intent(Intent.ACTION_VIEW,new Uri.Builder().scheme("market").authority("search").appendQueryParameter("q",query).appendQueryParameter("c","apps").build()).setPackage("com.android.vending");
            try { activity.startActivity(store); }
            catch (ActivityNotFoundException error) { SystemLinks.open(activity,new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/search").buildUpon().appendQueryParameter("q",query).appendQueryParameter("c","apps").build())); }
            return;
        }
        boolean music=app.equals("spotify"); String pkg=music ? "com.spotify.tv.android" : "com.stremio.one";
        Uri uri=music ? Uri.parse("spotify:search:"+Uri.encode(query)) : Uri.parse("stremio:///search").buildUpon().appendQueryParameter("search",query).build();
        try { activity.startActivity(new Intent(Intent.ACTION_VIEW,uri).setPackage(pkg)); }
        catch (ActivityNotFoundException | SecurityException error) {
            Intent fallback=activity.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (fallback!=null) {
                fallback.putExtra(android.app.SearchManager.QUERY,query); SystemLinks.open(activity,fallback);
                Toast.makeText(activity,TvUi.label(activity,"Search shortcut unavailable in this app version.","Esta versión de la app no ofrece el acceso a búsqueda."),Toast.LENGTH_LONG).show();
            } else Toast.makeText(activity,activity.getString(R.string.hc_unavailable),Toast.LENGTH_SHORT).show();
        }
    }
}
