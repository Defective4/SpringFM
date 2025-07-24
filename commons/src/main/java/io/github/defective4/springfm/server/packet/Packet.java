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

    public static final byte TYPE_AUDIO = 2;
    public static final byte TYPE_PAYLOAD = 1;

    private final byte[] payload;
    private final byte type;

    public Packet(byte[] audioSample) {
        payload = Objects.requireNonNull(audioSample);
        type = TYPE_AUDIO;
    }

    public Packet(PacketPayload payload) {
        this.payload = new Gson().toJson(Objects.requireNonNull(payload)).getBytes(StandardCharsets.UTF_8);
        type = TYPE_PAYLOAD;
    }

    private Packet(byte[] payload, byte type) {
        this.payload = payload;
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public JsonObject getPayloadAsJSON() {
        return JsonParser.parseString(new String(payload, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public byte getType() {
        return type;
    }

    public void toStream(DataOutputStream output) throws IOException {
        output.writeInt(payload.length);
        output.writeByte(type);
        output.write(payload);
    }

    public static Packet fromStream(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0) throw new IOException("Received invalid packet length: " + len);
        byte type = in.readByte();
        if (type < 1 || type > 2) throw new IOException("Received invalid packed ID: 0x" + Integer.toHexString(type));
        byte[] payload = new byte[len];
        in.readFully(payload);
        return new Packet(payload, type);
    }

}
