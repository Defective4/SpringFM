package io.github.defective4.springfm.server.service.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.StreamingAnnotationProcessor;
import io.github.defective4.springfm.server.processing.impl.RedseaRDSProcessor;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.util.RateLimiter;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class TestAnalogService implements AnalogRadioService {
    private InputStream audioInput;
    private float freq = 88e6f;
    private DataGenerator generator;
    private final int index = 0;
    private final File input;
    private final RateLimiter limiter = new RateLimiter(171000 * 2);
    private final float maxFreq = 1075e5f;
    private final float minFreq = 87e6f;

    private final String name;

    private final StreamingAnnotationProcessor processor;

    private Future<?> task;

    public TestAnalogService(File input, String name) {
        processor = new RedseaRDSProcessor(annotation -> {
            if (generator != null) synchronized (generator) {
                generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
            }
        });
        this.input = input;
        this.name = name;
    }

    @Override
    public float getCurrentFrequency() {
        return freq;
    }

    @Override
    public float getFrequencyStep() {
        return 100e3f;
    }

    @Override
    public float getMaxFrequency() {
        return maxFreq;
    }

    @Override
    public float getMinFrequency() {
        return minFreq;
    }

    @Override
    public String getName() {
        return name;
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
        audioInput = new BufferedInputStream(new FileInputStream(input));
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
    public void tune(float freq) {
        this.freq = freq;
    }

}
