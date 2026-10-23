package com.gathin.tvhome;

import android.widget.LinearLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public final class LightsActivity extends ControlPanelActivity {
    private final WizClient client = new WizClient();
    private final Map<String,LightCard> cards = new HashMap<>();
    private List<WizBulb> devices;
    private WizStore store;
    private LinearLayout lights;
    private boolean busy;
    private android.widget.TextView allPower;
    @Override protected void buildContent() { header(TvUi.label(this,"Lights","Luces"));
        if (store==null) { store = new WizStore(this); devices = store.load(); }
        allPower=TvUi.button(this,"",palette,() -> power(new ArrayList<>(devices),!allOn()),20);
        content.addView(allPower,new LinearLayout.LayoutParams(-1,-2)); TvUi.gap(content,16);
        lights=TvUi.column(this); content.addView(lights); rebuild();
        TvUi.gap(content,16);
        LinearLayout tools=row();
        android.widget.TextView sync=TvUi.button(this,TvUi.label(this,"Sync","Sincronizar"),palette,() -> refresh(false));
        android.widget.TextView scan=TvUi.button(this,TvUi.label(this,"Scan","Buscar luces"),palette,() -> refresh(true));
        LinearLayout.LayoutParams first=new LinearLayout.LayoutParams(0,dp(48),1); first.setMarginEnd(dp(10));
        tools.addView(sync,first); tools.addView(scan,new LinearLayout.LayoutParams(0,dp(48),1));
        render();
    }
    @Override protected void onStart() { super.onStart(); refresh(devices.isEmpty()); }
    @Override protected void onStop() { busy = false; super.onStop(); }
    private void rebuild() {
        lights.removeAllViews(); cards.clear();
        if (devices.isEmpty()) lights.addView(text(getString(R.string.hc_lights_empty),16));
        for (WizBulb device : devices) {
            LightCard card = new LightCard(this,device,() -> toggle(device),() -> rename(device));
            LinearLayout.LayoutParams bounds=new LinearLayout.LayoutParams(-1,-2); bounds.bottomMargin=dp(4);
            lights.addView(card,bounds); cards.put(device.mac,card);
        }
    }
    private void rename(WizBulb device) {
        if (busy) return;
        TvTextDialog.show(this,TvUi.label(this,"Rename light","Renombrar luz"),device.alias,
                TvUi.label(this,"Light name","Nombre de la luz"),false,48,alias -> {
            if (alias.isEmpty()) return TvUi.label(this,"Enter a name","Ingresá un nombre");
            device.alias=alias; store.save(devices); render(); return null;
        });
    }
    private void refresh(boolean discover) {
        if (busy) return;
        busy = true; status.setText(R.string.hc_loading); int request = generation;
        List<WizBulb> known = new ArrayList<>(devices);
        network.execute(() -> {
            List<WizBulb> found = known;
            try { if (discover) found = client.discover(known); } catch (IOException ignored) { }
            for (WizBulb device : found) client.refresh(device);
            List<WizBulb> result = found;
            main.post(() -> {
                if (!visible || generation != request) return;
                busy = false; devices = result; store.save(devices); if (discover) rebuild(); render();
                long online = devices.stream().filter(d -> d.reachable).count();
                status.setText(TvUi.label(this,"WiZ · Local network · ","WiZ · Red local · ") + online + "/" + devices.size());
            });
        });
    }
    private void toggle(WizBulb device) {
        if (!device.reachable) { refresh(false); return; }
        List<WizBulb> selected = new ArrayList<>(); selected.add(device);
        power(selected,!device.pilot.optBoolean("state"));
    }
    private void power(List<WizBulb> selected,boolean on) {
        if (busy || selected.isEmpty()) return;
        busy = true; status.setText(R.string.hc_loading);
        for (WizBulb device : selected) { LightCard card = cards.get(device.mac); if (card != null) card.show(device,true); }
        int request = generation;
        network.execute(() -> {
            boolean success = true;
            for (WizBulb device : selected) success = client.apply(device,powerParams(on)) && success;
            boolean confirmed = success;
            main.post(() -> {
                if (!visible || generation != request) return;
                busy = false; render(); status.setText(confirmed ? R.string.hc_command_ok : R.string.hc_command_failed);
            });
        });
    }
    private boolean allOn() { return !devices.isEmpty() && devices.stream().allMatch(d -> d.reachable && d.pilot.optBoolean("state")); }
    private void render() {
        for (WizBulb device:devices) { LightCard card=cards.get(device.mac); if (card!=null) card.show(device,busy); }
        allPower.setText(TvUi.label(this,allOn() ? "All lights OFF" : "All lights ON",allOn() ? "Apagar todas las luces" : "Encender todas las luces"));
        int foreground=palette.light ? 0xFFFFFFFF : palette.onAccent;
        allPower.setBackground(GlassMaterial.emphasis(allPower,palette,20,true)); allPower.setTextColor(foreground);
        TvUi.buttonIcons(allPower,R.drawable.lr_bulb,allOn() ? R.drawable.lr_power_off : R.drawable.lr_power_on,foreground);
        allPower.setClickable(!busy && !devices.isEmpty()); allPower.setAlpha(busy ? .65f : 1);
    }
    private static JSONObject powerParams(boolean on) {
        try { return new JSONObject().put("state",on); } catch (JSONException error) { throw new IllegalStateException(error); }
    }
}
