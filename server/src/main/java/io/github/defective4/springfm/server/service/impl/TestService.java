package io.github.defective4.springfm.server.service.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.StreamingAnnotationProcessor;
import io.github.defective4.springfm.server.processing.impl.RedseaRDSProcessor;
import io.github.defective4.springfm.server.service.DigitalRadioService;
import io.github.defective4.springfm.server.util.RateLimiter;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class TestService implements DigitalRadioService {
    private InputStream audioInput;
    private DataGenerator generator;
    private int index = 0;
    private final File[] inputs;
    private final RateLimiter limiter = new RateLimiter(171000 * 2);
    private final String name;
    private final StreamingAnnotationProcessor processor;
    private Future<?> task;

    public TestService(File[] inputs, String name) {
        processor = new RedseaRDSProcessor(annotation -> {
            if (generator != null) synchronized (generator) {
                generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
            }
        });
        this.inputs = inputs;
        this.name = name;
    }

    @Override
    public int getCurrentStation() {
        return index;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getStations() {
        return Arrays.stream(inputs).map(File::getName).toList().toArray(new String[0]);
    }

    @Override
    public boolean isStarted() {
        return audioInput != null && task != null && !task.isCancelled() && processor != null && processor.isStarted();
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        processor.start();
        audioInput = new BufferedInputStream(new FileInputStream(inputs[index]));
        audioInput.mark(Integer.MAX_VALUE);
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = audioInput.read(buffer);
                    if (read <= 0) {
                        audioInput.reset();
                        continue;
                    }
                    limiter.limit(read);
                    processor.write(buffer, read);
                    if (generator != null) synchronized (generator) {
                        generator.audioSampleGenerated(buffer);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws IOException {
        if (!isStarted()) throw new IllegalStateException("Already stopped");
        try {
            audioInput.close();
            task.cancel(true);
            processor.stop();
        } finally {
            audioInput = null;
            task = null;
        }
    }

    @Override
    public void tune(int stationIndex) throws IOException {
        if (stationIndex < 0 || stationIndex >= inputs.length)
            throw new IllegalArgumentException("Station index out of bounds");
        index = 0;
        stop();
        start();
    }
}
