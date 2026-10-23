package com.gathin.tvhome;

import android.os.SystemClock;
import android.widget.TextView;
import java.net.URLEncoder;
import org.json.JSONObject;

/** Official device-code flow. No password or existing app's private storage is read. */
public final class StremioLinkActivity extends ControlPanelActivity {
    private TextView code, address;
    private String pending = "";
    private long expires;
    private boolean loading;
    private final Runnable poll = this::read;
    @Override protected void buildContent() { header("Stremio");
        CharSequence previousCode=code==null ? "—" : code.getText();
        CharSequence previousAddress=address==null ? "" : address.getText();
        StremioStore store = new StremioStore(this);
        if (store.linked()) {
            status.setText(TvUi.label(this,"Account linked","Cuenta vinculada"));
            content.addView(text(TvUi.label(this,"Only your Continue Watching list appears on Home.",
                    "En el inicio solo aparece tu lista de Seguir viendo."),16));
            item(TvUi.label(this,"Disconnect this launcher","Desvincular este launcher"),
                    TvUi.label(this,"Your Stremio session stays signed in.","Tu sesión de Stremio sigue abierta."),
                    () -> TvUi.dialog(this).setTitle(TvUi.label(this,"Disconnect?","¿Desvincular?"))
                        .setPositiveButton(android.R.string.ok,(d,w) -> { store.unlink(); finish(); })
                        .setNegativeButton(android.R.string.cancel,null).show());
            return;
        }
        content.addView(text(TvUi.label(this,"Open the link below on your phone or computer, sign in to Stremio and enter this code.",
                "Abrí el enlace en tu celular o computadora, iniciá sesión en Stremio e ingresá este código."),16));
        TvUi.gap(content,24);
        code = TvUi.title(this,previousCode,38,palette.text); code.setLetterSpacing(.12f); content.addView(code);
        TvUi.gap(content,18);
        address = TvUi.text(this,previousAddress,16,palette.muted); content.addView(address);
        if (loading) status.setText(R.string.hc_loading);
        else if (!pending.isEmpty()) status.setText(TvUi.label(this,"Waiting for authorization…","Esperando autorización…"));
        item(TvUi.label(this,"New code","Nuevo código"),"",this::create);
    }
    @Override protected void onStart() { super.onStart(); if (code != null) create(); }
    @Override protected void onStop() { pending = ""; loading = false; super.onStop(); }
    private void create() {
        if (loading || !visible) return;
        main.removeCallbacks(poll); pending = ""; loading = true; int request = ++generation;
        status.setText(R.string.hc_loading); code.setText("—"); address.setText("");
        network.execute(() -> {
            try {
                JSONObject data = createCode();
                String value = data.getString("code");
                String link = data.getString("link");
                main.post(() -> {
                    if (!visible || request != generation) return;
                    loading = false; pending = value; expires = SystemClock.elapsedRealtime() + 5 * 60000;
                    code.setText(value); address.setText(link);
                    status.setText(TvUi.label(this,"Waiting for authorization…","Esperando autorización…"));
                    main.postDelayed(poll,3000);
                });
            } catch (Exception error) { main.post(() -> failed(request)); }
        });
    }
    private static JSONObject createCode() throws Exception {
        JSONObject data=NetJson.get("https://link.stremio.com/api/v2/create?type=Create","").getJSONObject("result");
        String value=data.getString("code");
        android.net.Uri link=android.net.Uri.parse(data.getString("link")); String host=link.getHost();
        if (!value.matches("[A-Za-z0-9-]{4,32}") || !"https".equals(link.getScheme())) throw new java.io.IOException("Invalid link");
        if (host==null || !(host.equals("stremio.com") || host.endsWith(".stremio.com"))) throw new java.io.IOException("Invalid link");
        return data;
    }
    private void failed(int request) {
        if (!visible || request != generation) return;
        loading = false; status.setText(TvUi.label(this,"Could not connect. Choose New code to retry.","No se pudo conectar. Elegí Nuevo código para reintentar."));
    }
    private void read() {
        if (!visible || pending.isEmpty()) return;
        if (SystemClock.elapsedRealtime() > expires) {
            pending = ""; code.setText("—");
            status.setText(TvUi.label(this,"Code expired. Choose New code.","Código vencido. Elegí Nuevo código.")); return;
        }
        final String value = pending; final int request = generation;
        network.execute(() -> {
            String key = "";
            try {
                JSONObject result = NetJson.get("https://link.stremio.com/api/v2/read?type=Read&code="
                        + URLEncoder.encode(value,"UTF-8"),"").optJSONObject("result");
                if (result != null) key = result.optString("authKey","");
            } catch (Exception ignored) { }
            final String auth = key;
            main.post(() -> accept(request,value,auth));
        });
    }
    private void accept(int request,String value,String auth) {
        if (!visible || request!=generation || !value.equals(pending)) return;
        if (auth.length()>=16 && auth.length()<=4096) {
            new StremioStore(this).link(auth); pending=""; code.setText("✓"); address.setText("");
            status.setText(TvUi.label(this,"Account linked","Cuenta vinculada")); main.postDelayed(this::finish,900);
        } else main.postDelayed(poll,4000);
    }
}
