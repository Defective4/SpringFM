package io.github.defective4.springfm.server.service;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.packet.DataGenerator;

public sealed interface RadioService permits DigitalRadioService, AnalogRadioService {
    AudioFormat getAudioFormat();

    String getName();

    boolean isStarted();

    void setPacketGenerator(DataGenerator generator);

    void start() throws IOException;

    void stop() throws IOException;
}
