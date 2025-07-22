package io.github.defective4.springfm.server.packet;

import java.util.Objects;

public class PacketPayload {
    private final String key;
    private final Object payload;

    protected PacketPayload(Object payload) {
        this.payload = Objects.requireNonNull(payload);
        key = PayloadRegistry.getKeyForClass(this.getClass());
    }

    public String getKey() {
        return key;
    }

    public Object getPayload() {
        return payload;
    }

}
