package io.github.defective4.springfm.server.service;

import java.io.IOException;
import java.util.Collection;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.packet.DataGenerator;

public sealed interface RadioService permits DigitalRadioService, AnalogRadioService {
    Collection<String> checkMissingDependencies();

    AudioFormat getAudioFormat();

    String getName();

    boolean isStarted();

    void setDebugMode(boolean debug);

    void setPacketGenerator(DataGenerator generator);

    void start() throws IOException;

    void stop() throws IOException;
}
