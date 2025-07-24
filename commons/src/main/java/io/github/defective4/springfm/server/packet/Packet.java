package io.github.defective4.springfm.server.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Packet {
    private final byte[] payload;

    public Packet(PacketPayload payload) {
        this.payload = new Gson().toJson(Objects.requireNonNull(payload)).getBytes(StandardCharsets.UTF_8);
    }

    private Packet(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getPayload() {
        return payload;
    }

    public JsonObject getPayloadAsJSON() {
        return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public void toStream(DataOutputStream output) throws IOException {
        output.writeInt(payload.length);
        output.write(payload);
    }

    public static Packet fromStream(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0) throw new IOException("Received invalid packet length: " + len);
        byte[] payload = new byte[len];
        in.readFully(payload);
        return new Packet(payload);
    }

}
