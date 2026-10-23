package com.gathin.tvhome;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import java.util.concurrent.TimeUnit;

/** Read-only connection labels. The audio label reports the selected TCL output, not an audible test. */
final class ConnectionStatus {
    final String wifi,audio;
    ConnectionStatus(String wifi,String audio) { this.wifi=wifi; this.audio=audio; }
    static ConnectionStatus read(Context context) { return new ConnectionStatus(wifi(context),audio(context)); }
    private static String wifi(Context context) {
        ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager==null) return "—";
        NetworkCapabilities caps=manager.getNetworkCapabilities(manager.getActiveNetwork());
        if (caps==null) return TvUi.label(context,"Disconnected","Sin conexión");
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
        WifiInfo info=wifiInfo(context);
        String name=ssid(info);
        if (!name.isEmpty()) return name;
        boolean connected=caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || (info!=null && info.getSupplicantState()==android.net.wifi.SupplicantState.COMPLETED);
        if (connected) return missingName(context);
        return TvUi.label(context,"Connected","Conectado");
    }
    private static WifiInfo wifiInfo(Context context) {
        try {
            WifiManager wifi=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            return wifi==null ? null : wifi.getConnectionInfo();
        } catch (SecurityException unavailable) { return null; }
    }
    private static String ssid(WifiInfo info) {
        String name=info==null ? null : info.getSSID();
        if (name==null || name.isEmpty() || name.equals(WifiManager.UNKNOWN_SSID)) return "";
        if (name.length()>1 && name.startsWith("\"") && name.endsWith("\"")) return name.substring(1,name.length()-1);
        return name;
    }
    private static String missingName(Context context) {
        if (!NetworkNameAccess.granted(context)) return TvUi.label(context,"Allow Wi-Fi name access","Permitir nombre de Wi-Fi");
        if (!NetworkNameAccess.locationEnabled(context)) return TvUi.label(context,"Enable location","Habilitar ubicación");
        return TvUi.label(context,"Network name unavailable","Nombre de red no disponible");
    }
    private static String audio(Context context) {
        int route=Settings.Global.getInt(context.getContentResolver(),"tcl_device_out_type",-1);
        String label;
        switch (route) {
            case 1: label=TvUi.label(context,"TV speakers","Altavoces TV"); break;
            case 16: label="ARC"; break;
            case 32: label=TvUi.label(context,"Optical","Óptico"); break;
            default: return TvUi.label(context,"System output","Salida del sistema");
        }
        String format=property("persist.tcl.SpdifType");
        return label+(format.equals("1") ? " · PCM" : "");
    }
    private static String property(String name) {
        Process process=null;
        try {
            process=new ProcessBuilder("/system/bin/getprop",name).start();
            if (!process.waitFor(1,TimeUnit.SECONDS)) return "";
            byte[] result=NetJson.read(process.getInputStream(),512);
            return new String(result,java.nio.charset.StandardCharsets.UTF_8).trim();
        } catch (java.io.IOException ignored) { return ""; }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return ""; }
        finally { if (process!=null) process.destroy(); }
    }
}
