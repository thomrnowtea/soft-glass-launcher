package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Locale;

final class WizBulb {
    final String mac;
    String host;
    String module;
    String alias;
    JSONObject pilot = new JSONObject();
    boolean reachable;
    boolean rgb;
    boolean white;
    boolean capabilitiesKnown;
    int minTemp = 2200;
    int maxTemp = 6500;

    WizBulb(String host, JSONObject config) throws JSONException {
        mac = config.getString("mac").replace(":", "").toLowerCase(Locale.ROOT);
        if (!mac.matches("[0-9a-f]{12}")) throw new JSONException("Invalid WiZ identity");
        this.host = host;
        module = config.optString("moduleName", config.optString("module", ""));
        alias = config.optString("alias", "WiZ " + mac.substring(8).toUpperCase(Locale.ROOT));
        rgb = module.contains("RGB");
        white = module.contains("C");
        capabilitiesKnown = config.optBoolean("capabilitiesKnown", false);
        white = config.optBoolean("white", white);
        minTemp = config.optInt("minTemp", minTemp);
        maxTemp = config.optInt("maxTemp", maxTemp);
    }

    JSONObject stored() throws JSONException {
        return new JSONObject().put("mac", mac).put("host", host).put("module", module)
                .put("alias", alias).put("white", white).put("minTemp", minTemp)
                .put("maxTemp", maxTemp).put("capabilitiesKnown", capabilitiesKnown);
    }
}
