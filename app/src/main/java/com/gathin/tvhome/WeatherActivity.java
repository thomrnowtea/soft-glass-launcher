package com.gathin.tvhome;

import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;

public final class WeatherActivity extends ControlPanelActivity {
    private WeatherRepository repository;
    private LinearLayout body;
    private boolean busy;
    private WeatherData current;
    @Override protected int panelWidth() { return 390; }
    @Override protected void buildContent() { header(TvUi.label(this,"Weather","Clima"));
        content.setPadding(dp(22),dp(20),dp(22),dp(16));
        if (repository==null) repository = new WeatherRepository(this); body = TvUi.column(this); content.addView(body);
        LinearLayout footer = row();
        footer.addView(TvUi.plain(this,getString(R.string.hc_refresh),() -> refresh(true)),new LinearLayout.LayoutParams(0,-2,1));
        footer.addView(TvUi.text(this,"Open-Meteo · CC BY 4.0",12,palette.muted)); show(current==null ? repository.cached() : current);
    }
    @Override protected void onStart() { super.onStart(); refresh(false); }
    @Override protected void onStop() { busy = false; super.onStop(); }
    private void refresh(boolean force) {
        if (!WeatherLocation.configured(this)) { show(null); return; }
        if (busy) return;
        busy = true; status.setText(R.string.hc_loading); int request = generation;
        network.execute(() -> {
            WeatherData data; boolean offline = false;
            try { data = repository.load(force); } catch (IOException error) { data = repository.cached(); offline = true; }
            WeatherData result = data; boolean failed = offline;
            main.post(() -> {
                if (!visible || generation != request) return;
                busy = false; show(result); if (failed) status.append(" · " + getString(R.string.hc_offline));
            });
        });
    }
    private void show(WeatherData data) {
        current=data;
        body.removeAllViews();
        WeatherLocation place=WeatherLocation.load(this); status.setText(place.name+" · "+place.detail);
        if (!WeatherLocation.configured(this)) {
            body.addView(TvUi.button(this,TvUi.label(this,"Choose a city","Elegir una ciudad"),palette,
                    () -> startActivity(new android.content.Intent(this,WeatherLocationActivity.class)))); return;
        }
        if (data == null) { body.addView(text(getString(R.string.hc_unavailable),14)); return; }
        LinearLayout current = TvUi.row(this);
        current.addView(TvUi.text(this,Math.round(data.temperature) + "°",58,palette.text),new LinearLayout.LayoutParams(0,dp(66),1));
        current.addView(new WeatherIcon(this,data.code),TvUi.params(this,61,65,0)); body.addView(current);
        TvUi.gap(body,7); body.addView(TvUi.text(this,WeatherData.description(this,data.code),16,palette.muted));
        TvUi.gap(body,16); TvUi.line(body);
        LinearLayout metrics = TvUi.row(this); metrics.setPadding(0,dp(11),0,dp(11));
        JSONObject actual = data.json.optJSONObject("current");
        metric(metrics,TvUi.label(this,"Feels like","Sensación"),scalar(actual,"apparent_temperature","°"));
        JSONObject daily = data.json.optJSONObject("daily");
        metric(metrics,TvUi.label(this,"Rain","Lluvia"),WeatherData.number(daily,"precipitation_probability_max",0,"%"));
        metric(metrics,TvUi.label(this,"Wind","Viento"),scalar(actual,"wind_speed_10m"," km/h")); body.addView(metrics);
        TvUi.line(body); TvUi.gap(body,15);
        body.addView(ForecastCard.hours(this,data,6)); TvUi.gap(body,13); TvUi.line(body);
        JSONArray codes = daily == null ? null : daily.optJSONArray("weather_code");
        for (int i=0;i<3;i++) day(data,daily,codes,i);
        TvUi.gap(body,10); body.addView(TvUi.text(this,data.updated(this),14,palette.muted));
    }
    private void metric(LinearLayout row,String name,String value) {
        LinearLayout cell = TvUi.column(this); cell.addView(TvUi.text(this,name,14,palette.muted));
        TvUi.gap(cell,5); cell.addView(TvUi.title(this,value,16,palette.text)); TvAccessibility.group(cell,name+". "+value);
        row.addView(cell,new LinearLayout.LayoutParams(0,-2,1));
    }
    private String scalar(JSONObject data,String field,String unit) {
        double value = data == null ? Double.NaN : data.optDouble(field,Double.NaN);
        return Double.isFinite(value) ? Math.round(value) + unit : "—";
    }
    private void day(WeatherData data,JSONObject daily,JSONArray codes,int index) {
        LinearLayout row = TvUi.row(this); row.setPadding(0,dp(3),0,dp(3));
        TextView title = TvUi.text(this,index==0 ? TvUi.label(this,"Today","Hoy") : data.dayTitle(index).split(" ")[0],16,palette.text);
        row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        row.addView(new WeatherIcon(this,codes == null ? -1 : codes.optInt(index,-1)),TvUi.params(this,23,25,12));
        row.addView(TvUi.text(this,WeatherData.number(daily,"temperature_2m_min",index,"°") + "    "
                + WeatherData.number(daily,"temperature_2m_max",index,"°"),16,palette.text),TvUi.params(this,90,-2,8));
        TextView rain = TvUi.text(this,WeatherData.number(daily,"precipitation_probability_max",index,"%"),14,palette.muted);
        row.addView(rain,TvUi.params(this,40,-2,0));
        TvAccessibility.group(row,title.getText()+". "+WeatherData.number(daily,"temperature_2m_min",index,"°")+". "
                +WeatherData.number(daily,"temperature_2m_max",index,"°")+". "+TvUi.label(this,"Rain ","Lluvia ")+rain.getText()); body.addView(row);
    }
}
