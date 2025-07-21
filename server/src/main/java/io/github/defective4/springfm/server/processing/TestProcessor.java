package io.github.defective4.springfm.server.processing;

import java.io.IOException;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.data.AudioAnnotation;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.PacketGenerator;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class TestProcessor implements AnnotationProcessor {
    private final PacketGenerator generator;
    private Future<?> task;

    public TestProcessor(PacketGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        task = ThreadUtils.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                generator.packetGenerated(
                        new Packet(new AudioAnnotationPayload(new AudioAnnotation("RMF FM", "Testowy opis"))));
            }
        });
    }

    @Override
    public void stop() throws IOException {
        task.cancel(true);
    }
}
