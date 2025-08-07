package io.github.defective4.springfm.server.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.google.gson.Gson;

public class Packet {
    private final PacketPayload payload;

    public Packet(PacketPayload payload) {
        this.payload = Objects.requireNonNull(payload);
    }

    public PacketPayload getPayload() {
        return payload;
    }

    public void toStream(DataOutputStream output) throws IOException {
        byte[] payload = new Gson().toJson(this.payload).getBytes(StandardCharsets.UTF_8);
        output.writeInt(payload.length);
        output.write(payload);
    }

    public static Packet fromStream(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0) throw new IOException("Received invalid packet length: " + len);
        byte[] payload = new byte[len];
        in.readFully(payload);
        return new Packet(new Gson().fromJson(new String(payload, StandardCharsets.UTF_8), PacketPayload.class));
    }
}
