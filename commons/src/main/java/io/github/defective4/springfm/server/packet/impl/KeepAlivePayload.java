package io.github.defective4.springfm.server.packet.impl;

import io.github.defective4.springfm.server.packet.PacketPayload;

public class KeepAlivePayload extends PacketPayload {
    public KeepAlivePayload(long l) {
        super("keepalive", l);
    }
}
