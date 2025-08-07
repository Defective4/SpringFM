package io.github.defective4.springfm.server.packet;

import java.util.Objects;

public class PacketPayload {
    private final String key;
    private final Object payload;

    protected PacketPayload(String key, Object payload) {
        this.payload = Objects.requireNonNull(payload);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public Object getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "PacketPayload [key=" + key + ", payload=" + payload + "]";
    }

}
