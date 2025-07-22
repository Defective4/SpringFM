package io.github.defective4.springfm.server.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;

public class PayloadRegistry {
    private static final Map<String, Class<? extends PacketPayload>> PACKET_MAP = new HashMap<>();

    static {
        PACKET_MAP.putAll(Map.of("annotation", AudioAnnotationPayload.class));
    }

    private PayloadRegistry() {
    }

    public static String getKeyForClass(Class<? extends PacketPayload> clazz) {
        Objects.requireNonNull(clazz);
        return PACKET_MAP.entrySet().stream().filter(e -> e.getValue().equals(clazz)).findAny().orElseThrow().getKey();
    }
}
