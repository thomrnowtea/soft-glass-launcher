package com.gathin.tvhome;

import android.net.Uri;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public final class WeatherLocationActivity extends ControlPanelActivity {
    private LinearLayout results;
    private int queryId;
    private List<WeatherLocation> previousResults;
    private boolean previousFailure;
    @Override protected void buildContent() { header(TvUi.label(this,"Weather location","Ciudad del pronóstico"));
        WeatherLocation current=WeatherLocation.load(this); status.setText(current.name+" · "+current.detail);
        content.addView(TvUi.button(this,TvUi.label(this,"Search city","Buscar ciudad"),palette,this::edit));
        TvUi.gap(content,16); results=TvUi.column(this); content.addView(results);
        if (previousResults!=null) show(previousResults,previousFailure,false);
    }
    private void edit() {
        TvTextDialog.show(this,TvUi.label(this,"Search city","Buscar ciudad"),"",TvUi.label(this,"City, country","Ciudad, país"),false,120,query -> {
            if (query.length()<2) return TvUi.label(this,"Enter at least two characters","Ingresá al menos dos caracteres");
            search(query); return null;
        });
    }
    private void search(String query) {
        int request=++queryId,life=generation; status.setText(R.string.hc_loading); results.removeAllViews();
        network.execute(() -> {
            List<WeatherLocation> found=new ArrayList<>(); boolean failed=false;
            try {
                JSONObject json=NetJson.get("https://geocoding-api.open-meteo.com/v1/search?name="+Uri.encode(query)
                        +"&count=8&language="+getResources().getConfiguration().getLocales().get(0).getLanguage()+"&format=json","");
                JSONArray entries=json.optJSONArray("results");
                if (entries!=null) for (int i=0;i<entries.length();i++) {
                    try { found.add(WeatherLocation.from(entries.getJSONObject(i))); } catch (Exception invalidPlace) { }
                }
            } catch (java.io.IOException unavailable) { failed=true; }
            boolean error=failed;
            main.post(() -> { if (visible && generation==life && queryId==request) show(found,error); });
        });
    }
    private void show(List<WeatherLocation> places,boolean failed) {
        show(places,failed,true);
    }
    private void show(List<WeatherLocation> places,boolean failed,boolean focusResults) {
        previousResults=places; previousFailure=failed; results.removeAllViews();
        status.setText(failed ? getString(R.string.hc_offline) : TvUi.label(this,"Select a city","Elegí una ciudad"));
        if (places.isEmpty() && !failed) status.setText(TvUi.label(this,"No cities found. Try city, country.","Sin resultados. Probá ciudad, país."));
        for (WeatherLocation place:places) {
            android.widget.TextView row=TvUi.plain(this,place.name+"\n"+place.detail,() -> select(place));
            results.addView(row,new LinearLayout.LayoutParams(-1,-2)); TvUi.line(results);
        }
        if (focusResults && results.getChildCount()>0) results.getChildAt(0).requestFocus();
    }
    private void select(WeatherLocation place) { place.save(this); new TvPreferences(this).widget(HomeWidget.WEATHER,true); finish(); }
}
