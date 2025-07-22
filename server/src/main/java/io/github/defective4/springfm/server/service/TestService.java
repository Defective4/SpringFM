package io.github.defective4.springfm.server.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.PacketGenerator;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.RedseaRDSProcessor;
import io.github.defective4.springfm.server.util.RateLimiter;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class TestService implements RadioService {
    private InputStream audioInput;
    private PacketGenerator generator;
    private final File input;
    private final RateLimiter limiter = new RateLimiter(171000 * 2);
    private final String name;
    private final RedseaRDSProcessor processor;
    private Future<?> task;

    public TestService(File input, String name) {
        processor = new RedseaRDSProcessor(annotation -> {
            if (generator != null) synchronized (generator) {
                generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
            }
        }, 171);
        this.input = input;
        this.name = name;
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
    public void setPacketGenerator(PacketGenerator generator) {
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
                    processor.writeData(buffer, read);
                    if (generator != null) synchronized (generator) {
                        generator.packetGenerated(new Packet(buffer));
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
        audioInput.close();
        task.cancel(true);
        processor.stop();
    }
}
