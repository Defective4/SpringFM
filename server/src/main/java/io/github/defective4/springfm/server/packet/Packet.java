package io.github.defective4.springfm.server.packet;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.Gson;

public class Packet {
    private static final byte TYPE_AUDIO = 2;
    private static final byte TYPE_PAYLOAD = 1;

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

    public byte[] getPayload() {
        return payload;
    }

    public byte getType() {
        return type;
    }

}
