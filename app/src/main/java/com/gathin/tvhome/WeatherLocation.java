package com.gathin.tvhome;

import android.content.Context;
import org.json.JSONObject;
import java.time.ZoneId;

/** A selected forecast place, independent of GPS and the TV's own clock/time zone. */
final class WeatherLocation {
    final String name,detail,timezone;
    final double latitude,longitude;
    WeatherLocation(String name,String detail,double latitude,double longitude,String timezone) {
        if (!Double.isFinite(latitude) || Math.abs(latitude)>90 || !Double.isFinite(longitude) || Math.abs(longitude)>180)
            throw new IllegalArgumentException("Invalid coordinates");
        ZoneId.of(timezone); this.name=name; this.detail=detail; this.latitude=latitude; this.longitude=longitude; this.timezone=timezone;
    }
    static WeatherLocation palermo() { return new WeatherLocation("Palermo","Buenos Aires, Argentina",-34.58856,-58.43053,"America/Argentina/Buenos_Aires"); }
    static boolean configured(Context context) {
        String value=context.getSharedPreferences("living_room",0).getString("weather_location","");
        try { from(new JSONObject(value)); return true; } catch (Exception missing) { return false; }
    }
    static WeatherLocation load(Context context) {
        String saved=context.getSharedPreferences("living_room",0).getString("weather_location","");
        try { return from(new JSONObject(saved)); } catch (Exception invalid) {
            return new WeatherLocation(TvUi.label(context,"Choose city","Elegir ciudad"),"",0,0,ZoneId.systemDefault().getId());
        }
    }
    static WeatherLocation from(JSONObject value) throws org.json.JSONException {
        String name=value.getString("name"),region=value.optString("admin1"),country=value.optString("country");
        String detail=region.isEmpty() ? country : region+(country.isEmpty() ? "" : ", "+country);
        return new WeatherLocation(name,detail,value.getDouble("latitude"),value.getDouble("longitude"),value.optString("timezone","UTC"));
    }
    void save(Context context) {
        try {
            JSONObject value=new JSONObject().put("name",name).put("admin1",detail).put("latitude",latitude)
                    .put("longitude",longitude).put("timezone",timezone);
            context.getSharedPreferences("living_room",0).edit().putString("weather_location",value.toString()).apply();
        } catch (org.json.JSONException impossible) { throw new IllegalStateException(impossible); }
    }
    String signature() { return latitude+":"+longitude+":"+timezone; }
}
