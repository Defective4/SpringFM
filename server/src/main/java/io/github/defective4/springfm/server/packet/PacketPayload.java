package io.github.defective4.springfm.server.packet;

public class PacketPayload {
    private final String key;
    private final Object payload;

    protected PacketPayload(Object payload) {
        key = PayloadRegistry.getKeyForClass(this.getClass());
        this.payload = payload;
    }

    public String getKey() {
        return key;
    }

    public Object getPayload() {
        return payload;
    }

}
