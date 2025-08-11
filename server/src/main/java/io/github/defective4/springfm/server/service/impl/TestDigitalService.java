package io.github.defective4.springfm.server.service.impl;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.service.DigitalRadioService;

public class TestDigitalService implements DigitalRadioService {

    private final AudioFormat format;

    private int station;

    private final String[] stations = { "Station A", "Station B", "Station C" };

    public TestDigitalService(AudioFormat format) {
        this.format = format;
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public int getCurrentStation() {
        return station;
    }

    @Override
    public String getName() {
        return "Test service";
    }

    @Override
    public String[] getStations() {
        return stations;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void setDebugMode(boolean debug) {
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void stop() throws IOException {
    }

    @Override
    public void tune(int stationIndex) throws IllegalArgumentException, IOException {
        if (stationIndex >= stations.length || stationIndex < 0)
            throw new IllegalArgumentException("Station index out of bounds");
        station = stationIndex;
    }

}
