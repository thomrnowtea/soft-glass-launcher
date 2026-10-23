package com.gathin.tvhome;

import android.content.Context;

/** Independently visible sections below the app rail. Hiding one never changes the device state. */
enum HomeWidget {
    INPUTS("Inputs", "Entradas"),
    SEARCH("Search", "Buscar"),
    WEATHER("Weather", "Clima"),
    LIGHTS("Lights", "Luces"),
    CONNECTIONS("Connections", "Conexiones");

    private final String english, spanish;
    HomeWidget(String english, String spanish) { this.english=english; this.spanish=spanish; }
    String key() { return "widget_"+name().toLowerCase(java.util.Locale.ROOT); }
    String label(Context context) { return TvUi.label(context,english,spanish); }
}
