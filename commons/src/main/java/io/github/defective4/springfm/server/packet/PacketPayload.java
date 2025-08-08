package io.github.defective4.springfm.server.packet;

import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class PacketPayload {
    private static final Gson GSON = new Gson();
    private final String key;
    private final JsonElement payload;

    protected PacketPayload(String key, JsonElement payload) {
        this.payload = Objects.requireNonNull(payload);
        this.key = key;
    }

    protected PacketPayload(String key, Object payload) {
        this(key, GSON.toJsonTree(payload));

    }

    public String getKey() {
        return key;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public <T> T getPayloadAsObject(Class<T> type) {
        return GSON.fromJson(payload.getAsJsonObject(), type);
    }

    @Override
    public String toString() {
        return "PacketPayload [key=" + key + ", payload=" + payload + "]";
    }

}
