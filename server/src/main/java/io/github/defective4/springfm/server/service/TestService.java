package io.github.defective4.springfm.server.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.PacketGenerator;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.RedseaRDSProcessor;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class TestService implements RadioService {
    private final DataInputStream audioInput;
    private final PacketGenerator generator;
    private final RedseaRDSProcessor processor;
    private Future<?> task;

    public TestService(InputStream audioInput, PacketGenerator generator) {
        processor = new RedseaRDSProcessor(
                annotation -> generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation))), 171);
        this.audioInput = new DataInputStream(audioInput);
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        processor.start();
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (true) {
                    int read = audioInput.read(buffer);
                    if (read <= 0) break;
                    processor.writeData(buffer, read);
                    generator.packetGenerated(new Packet(buffer));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws IOException {
        audioInput.close();
        task.cancel(true);
        processor.stop();
    }
}
