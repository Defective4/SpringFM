package io.github.defective4.springfm.server.service;

import java.io.IOException;

import io.github.defective4.springfm.server.packet.PacketGenerator;

public interface RadioService {

    boolean isStarted();

    void setPacketGenerator(PacketGenerator generator);

    void start() throws IOException;

    void stop() throws IOException;
}
