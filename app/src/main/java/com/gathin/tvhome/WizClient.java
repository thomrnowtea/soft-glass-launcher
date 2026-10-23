package com.gathin.tvhome;

import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded request/response UDP; discovery is read-only and never registers a controller. */
final class WizClient {
    private static final int PORT = 38899;
    private static final AtomicInteger IDS = new AtomicInteger(100);

    List<WizBulb> discover(List<WizBulb> known) throws IOException {
        LinkedHashMap<String, WizBulb> bulbs = new LinkedHashMap<>();
        for (WizBulb bulb : known) bulbs.put(bulb.mac, bulb);
        int id = IDS.incrementAndGet();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);
            for (InetAddress address : broadcasts()) send(socket, address, request("getSystemConfig", new JSONObject(), id));
            collect(socket, bulbs, id);
        }
        return new ArrayList<>(bulbs.values());
    }

    private void collect(DatagramSocket socket, LinkedHashMap<String, WizBulb> bulbs, int id) throws IOException {
        long deadline = SystemClock.elapsedRealtime() + 1800;
        while (SystemClock.elapsedRealtime() < deadline && bulbs.size() < 32) {
            if (Thread.currentThread().isInterrupted()) return;
            socket.setSoTimeout((int) Math.max(1, deadline - SystemClock.elapsedRealtime()));
            try {
                DatagramPacket packet = receive(socket);
                addDiscovered(packet, bulbs, id);
            } catch (SocketTimeoutException complete) { return; }
        }
    }

    private void addDiscovered(DatagramPacket packet, LinkedHashMap<String, WizBulb> bulbs, int id) {
        if (!packet.getAddress().isSiteLocalAddress()) return;
        try {
            JSONObject response = decode(packet);
            if (!matches(response, "getSystemConfig", id)) return;
            WizBulb found = new WizBulb(packet.getAddress().getHostAddress(), response.getJSONObject("result"));
            WizBulb previous = bulbs.get(found.mac);
            if (previous == null) bulbs.put(found.mac, found);
            else previous.host = found.host;
        } catch (JSONException ignored) { /* Unrelated LAN traffic is not a light. */ }
    }

    boolean refresh(WizBulb bulb) {
        bulb.reachable = false;
        try {
            JSONObject state = query(bulb.host, "getPilot", new JSONObject());
            String identity = state.optString("mac", "").replace(":", "");
            if (!bulb.mac.equalsIgnoreCase(identity)) return false;
            if (!state.has("state")) return false;
            bulb.pilot = state;
            bulb.reachable = true;
            return true;
        } catch (IOException error) { return false; }
    }

    private void readCapabilities(WizBulb bulb) {
        if (bulb.capabilitiesKnown) return;
        try {
            JSONObject model = query(bulb.host, "getModelConfig", new JSONObject());
            JSONArray range = model.optJSONArray("cctRange");
            if (range != null && range.length() >= 4) {
                bulb.minTemp = range.optInt(0, 2200);
                bulb.maxTemp = range.optInt(3, 6500);
                bulb.white = bulb.maxTemp > bulb.minTemp;
            }
            bulb.capabilitiesKnown = true;
        } catch (IOException ignored) { /* State is usable; unsupported controls remain conservative. */ }
    }

    boolean apply(WizBulb bulb, JSONObject change) {
        if (!refresh(bulb)) return false; // Verify MAC before sending to a remembered DHCP address.
        try {
            JSONObject ack = query(bulb.host, "setPilot", change);
            if (!ack.optBoolean("success", false)) { bulb.reachable = false; return false; }
            if (!refresh(bulb)) return false;
            return confirms(bulb.pilot, change);
        } catch (IOException error) { bulb.reachable = false; return false; }
    }

    private static boolean confirms(JSONObject actual, JSONObject expected) {
        if (actual.optBoolean("state") != expected.optBoolean("state")) return false;
        for (String key : new String[]{"dimming", "temp", "r", "g", "b"}) {
            if (!expected.has(key)) continue;
            if (!actual.has(key)) return false;
            if (Math.abs(actual.optInt(key) - expected.optInt(key)) > 2) return false;
        }
        return true;
    }

    private JSONObject query(String host, String method, JSONObject params) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        if (!(address instanceof Inet4Address) || !address.isSiteLocalAddress()) throw new IOException("Local IPv4 required");
        int id = IDS.incrementAndGet();
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(address, PORT);
            socket.setSoTimeout(1200);
            send(socket, address, request(method, params, id));
            JSONObject response = decode(receive(socket));
            if (!matches(response, method, id) || response.has("error")) throw new IOException("Unexpected WiZ response");
            return response.getJSONObject("result");
        } catch (JSONException error) { throw new IOException("Invalid WiZ response", error); }
    }

    private static JSONObject request(String method, JSONObject params, int id) {
        try { return new JSONObject().put("id", id).put("method", method).put("params", params); }
        catch (JSONException impossible) { throw new IllegalStateException(impossible); }
    }

    private static boolean matches(JSONObject response, String method, int id) {
        return method.equals(response.optString("method")) && response.optInt("id", -1) == id;
    }

    private static void send(DatagramSocket socket, InetAddress address, JSONObject request) throws IOException {
        byte[] data = request.toString().getBytes(StandardCharsets.UTF_8);
        socket.send(new DatagramPacket(data, data.length, address, PORT));
    }

    private static DatagramPacket receive(DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[8192], 8192);
        socket.receive(packet);
        return packet;
    }

    private static JSONObject decode(DatagramPacket packet) throws JSONException {
        return new JSONObject(new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8));
    }

    private static Set<InetAddress> broadcasts() throws IOException {
        Set<InetAddress> addresses = new LinkedHashSet<>();
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) throw new IOException("No network interfaces");
        for (NetworkInterface network : Collections.list(interfaces)) {
            if (!network.isUp() || network.isLoopback()) continue;
            for (InterfaceAddress entry : network.getInterfaceAddresses()) {
                if (entry.getAddress().isSiteLocalAddress() && entry.getBroadcast() != null) addresses.add(entry.getBroadcast());
            }
        }
        if (addresses.isEmpty()) addresses.add(InetAddress.getByName("255.255.255.255"));
        return addresses;
    }
}
