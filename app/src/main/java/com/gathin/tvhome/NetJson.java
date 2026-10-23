package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class NetJson {
    static JSONObject post(String endpoint, JSONObject body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(6000); connection.setReadTimeout(6000);
        connection.setRequestMethod("POST"); connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(bytes.length);
        try {
            try (java.io.OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            if (connection.getResponseCode() != 200) throw new IOException("Service unavailable");
            try (InputStream input = connection.getInputStream()) {
                return new JSONObject(new String(read(input, 2 * 1024 * 1024), StandardCharsets.UTF_8));
            }
        } catch (JSONException error) { throw new IOException("Invalid response"); }
        finally { connection.disconnect(); }
    }
    static JSONObject get(String endpoint, String bearer) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(6000); connection.setReadTimeout(6000);
        connection.setRequestProperty("Accept", "application/json");
        if (!bearer.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + bearer);
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("Catalogue HTTP " + status);
            try (InputStream input = connection.getInputStream()) {
                return new JSONObject(new String(read(input, 512 * 1024), StandardCharsets.UTF_8));
            }
        } catch (JSONException error) { throw new IOException("Invalid catalogue data"); }
        finally { connection.disconnect(); }
    }
    static byte[] read(InputStream input, int max) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192]; int count;
        while ((count = input.read(chunk)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
            if (buffer.size() + count > max) throw new IOException("Response too large");
            buffer.write(chunk, 0, count);
        }
        return buffer.toByteArray();
    }
}
