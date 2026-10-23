package com.gathin.tvhome;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** This file stays inside app-private storage and is never bundled into the APK. */
final class TmdbAuth {
    private final File file;
    TmdbAuth(Context context) { file = new File(context.getFilesDir(), "tmdb-credential"); }
    File file() { return file; }
    String read() {
        try {
            if (!file.isFile() || file.length() > 4096) return "";
            String value = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim();
            return valid(value) ? value : "";
        } catch (IOException error) { return ""; }
    }
    boolean configured() { return !read().isEmpty(); }
    boolean save(String value) {
        value = value.trim();
        if (!valid(value)) return false;
        try { Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8)); return true; }
        catch (IOException error) { return false; }
    }
    static boolean valid(String value) {
        if (value.length() > 4096) return false;
        return value.matches("[a-fA-F0-9]{32}")
                || value.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    }
}
