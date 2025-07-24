package io.github.defective4.springfm.server.service;

import java.io.IOException;

import io.github.defective4.springfm.server.packet.PacketGenerator;

public sealed interface RadioService permits DigitalRadioService {

    String getName();

    boolean isStarted();

    void setPacketGenerator(PacketGenerator generator);

    void start() throws IOException;

    void stop() throws IOException;
}
