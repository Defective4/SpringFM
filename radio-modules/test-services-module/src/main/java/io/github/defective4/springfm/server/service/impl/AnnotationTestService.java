package io.github.defective4.springfm.server.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.github.defective4.springfm.server.data.AudioAnnotation;
import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.service.DigitalRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class AnnotationTestService implements DigitalRadioService {

    public static class AnnotationStation {
        private final String name;
        private final boolean nonMusic;
        private final String title, description;

        public AnnotationStation(String title, String description, boolean nonMusic, String name) {
            this.name = name;
            this.title = title;
            this.description = description;
            this.nonMusic = nonMusic;
        }

        public String getDescription() {
            return description;
        }

        public String getName() {
            return name;
        }

        public String getTitle() {
            return title;
        }

        public boolean isNonMusic() {
            return nonMusic;
        }

        public AudioAnnotation toAnnotation() {
            return new AudioAnnotation(title, description, nonMusic);
        }
    }

    private final long annotationInterval;
    private boolean debug;
    private final AudioFormat format;
    private DataGenerator generator;
    private final String name;
    private Future<?> scheduledTask;
    private int station = 0;
    private final List<AnnotationStation> stations;

    public AnnotationTestService(@ServiceArgument(name = "name") String name,
            @ServiceArgument(name = "stations") JsonArray stations,
            @ServiceArgument(name = "annotationInterval", defaultValue = "1000") Long annotationInterval,
            AudioFormat format) {
        this.format = format;
        if (name == null) throw new IllegalArgumentException("Service name is required");
        if (stations == null) throw new IllegalArgumentException("Stations configuration missing from config");
        this.name = name;
        this.annotationInterval = annotationInterval;
        List<AnnotationStation> list = new ArrayList<>();
        Gson gson = new Gson();
        for (JsonElement element : stations) {
            AnnotationStation station = gson.fromJson(element, AnnotationStation.class);
            if (station.getName() == null) throw new IllegalArgumentException("Station name is required");
            list.add(station);
        }
        this.stations = Collections.unmodifiableList(list);
    }

    @Override
    public Collection<String> checkMissingDependencies() {
        return Collections.emptyList();
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
        return name;
    }

    @Override
    public String[] getStations() {
        return stations.stream().map(AnnotationStation::getName).toList().toArray(new String[0]);
    }

    @Override
    public void init() throws IOException {
    }

    @Override
    public boolean isStarted() {
        return scheduledTask != null && !scheduledTask.isCancelled();
    }

    @Override
    public void setDebugMode(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        scheduledTask = ThreadUtils.submit(() -> {
            while (isStarted()) {
                generator.packetGenerated(new Packet(new AudioAnnotationPayload(stations.get(station).toAnnotation())));
                try {
                    Thread.sleep(annotationInterval);
                } catch (InterruptedException e) {}
            }
        });
    }

    @Override
    public void stop() throws IOException {
        if (scheduledTask != null) scheduledTask.cancel(true);
        scheduledTask = null;
    }

    @Override
    public void tune(int stationIndex) throws IllegalArgumentException, IOException {
        station = stationIndex;
    }

}
