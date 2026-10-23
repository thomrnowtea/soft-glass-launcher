package com.gathin.tvhome;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;

/** Static native forecast: open columns, custom weather artwork and honest cache age. */
final class ForecastCard extends LinearLayout {
    private final boolean compact;
    private final TvPalette palette;
    ForecastCard(Context context,boolean compact) {
        super(context); this.compact = compact; palette = TvPalette.of(context); setOrientation(VERTICAL);
        if (compact) {
            setPadding(dp(16),dp(16),dp(16),dp(16)); setMinimumHeight(dp(170));
            setBackground(GlassMaterial.card(this,palette,12));
        }
    }
    void show(WeatherData data) {
        removeAllViews();
        if (compact) {
            LinearLayout heading = TvUi.row(getContext());
            heading.addView(TvUi.title(getContext(),WeatherLocation.load(getContext()).name,18,palette.text),new LayoutParams(0,-2,1));
            heading.addView(TvUi.text(getContext(),TvUi.label(getContext(),"Weather  ›","Clima  ›"),14,palette.muted)); addView(heading); TvUi.gap(this,14);
        }
        if (data == null) { addView(TvUi.text(getContext(),getContext().getString(R.string.hc_unavailable),13,palette.muted)); return; }
        if (compact) {
            compactBody(data);
        } else {
            addView(current(data)); TvUi.gap(this,15); addView(hours(getContext(),data,6));
        }
        if (!compact) { TvUi.gap(this,16); addView(TvUi.text(getContext(),data.updated(getContext()),14,palette.muted)); }
        setContentDescription(WeatherLocation.load(getContext()).name+". "+data.compact(getContext())+". "+data.updated(getContext()));
    }
    private void compactBody(WeatherData data) {
        if (getResources().getConfiguration().fontScale>1.2f) {
            addView(current(data)); TvUi.gap(this,16); addView(hours(getContext(),data,4)); return;
        }
        LinearLayout body=TvUi.row(getContext()); body.setGravity(Gravity.TOP);
        body.addView(current(data),TvUi.params(getContext(),120,-2,10));
        body.addView(hours(getContext(),data,4),new LayoutParams(0,-2,1)); addView(body);
    }
    private LinearLayout current(WeatherData data) {
        LinearLayout column = TvUi.column(getContext()), row = TvUi.row(getContext());
        row.addView(new WeatherIcon(getContext(),data.code),TvUi.params(getContext(),compact ? 34 : 58,compact ? 42 : 65,7));
        row.addView(TvUi.text(getContext(),Math.round(data.temperature) + "°",compact ? 41 : 58,palette.text)); column.addView(row);
        column.addView(TvUi.text(getContext(),WeatherData.description(getContext(),data.code),16,palette.muted)); TvUi.gap(column,8);
        JSONObject daily = data.json.optJSONObject("daily");
        column.addView(TvUi.text(getContext(),"↑ " + WeatherData.number(daily,"temperature_2m_max",0,"°") + "   ↓ "
                + WeatherData.number(daily,"temperature_2m_min",0,"°"),14,palette.muted)); return column;
    }
    static LinearLayout hours(Context context,WeatherData data,int limit) {
        LinearLayout result = TvUi.column(context);
        JSONObject hourly = data.json.optJSONObject("hourly");
        if (hourly == null) return result;
        JSONArray times = hourly.optJSONArray("time"), codes = hourly.optJSONArray("weather_code");
        if (times == null) return result;
        String now = LocalDateTime.now(data.zone).withMinute(0).withSecond(0).withNano(0).toString();
        int count = 0, columns=limit>4 ? 3 : Math.max(1,limit); LinearLayout row=null;
        for (int i=0; i<times.length() && count<limit; i++) {
            String time = times.optString(i); if (time.compareTo(now)<0) continue;
            if (count%columns==0) { TvUi.gap(result,8); row=TvUi.row(context); result.addView(row); }
            row.addView(hour(context,hourly,codes,i,time),new LayoutParams(0,-2,1)); count++;
        }
        return result;
    }
    private static LinearLayout hour(Context context,JSONObject hourly,JSONArray codes,int index,String rawTime) {
        TvPalette palette=TvPalette.of(context); LinearLayout cell=TvUi.column(context); cell.setGravity(Gravity.CENTER);
        String time=rawTime.length()>=16 ? rawTime.substring(11,16) : "—";
        String temperature=WeatherData.number(hourly,"temperature_2m",index,"°");
        String rain=WeatherData.number(hourly,"precipitation_probability",index,"%");
        cell.addView(TvUi.text(context,time,14,palette.muted));
        cell.addView(new WeatherIcon(context,codes==null ? -1 : codes.optInt(index,-1)),TvUi.params(context,26,32,0));
        cell.addView(TvUi.title(context,temperature,16,palette.text)); TvUi.gap(cell,5);
        if (context instanceof WeatherActivity) cell.addView(TvUi.text(context,rain,14,palette.muted));
        TvAccessibility.group(cell,time+". "+temperature+". "+TvUi.label(context,"Rain ","Lluvia ")+rain); return cell;
    }
    private int dp(float value) { return TvUi.dp(getContext(),value); }
}
