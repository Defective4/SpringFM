package io.github.defective4.springfm.server.packet;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

public class Packet {
    private static final byte TYPE_AUDIO = 2;
    private static final byte TYPE_PAYLOAD = 1;

    private final byte[] payload;
    private final byte type;

    public Packet(byte[] audioSample) {
        type = TYPE_AUDIO;
        payload = audioSample;
    }

    public Packet(PacketPayload payload) {
        type = TYPE_PAYLOAD;
        this.payload = new Gson().toJson(payload).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte getType() {
        return type;
    }

}
