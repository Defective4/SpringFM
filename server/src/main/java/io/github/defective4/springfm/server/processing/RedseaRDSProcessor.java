package io.github.defective4.springfm.server.processing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Future;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.springfm.server.data.AnnotationGenerator;
import io.github.defective4.springfm.server.data.AudioAnnotation;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class RedseaRDSProcessor implements AnnotationProcessor {

    private final AnnotationGenerator generator;
    private OutputStream os;
    private Process redsea;

    private final int sampleRateKHz;

    private Future<?> task;

    public RedseaRDSProcessor(AnnotationGenerator generator, int sampleRateKHz) {
        this.generator = Objects.requireNonNull(generator);
        this.sampleRateKHz = sampleRateKHz;
    }

    @Override
    public boolean isStarted() {
        return task != null && os != null && redsea != null && redsea.isAlive() && !task.isCancelled();
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        redsea = new ProcessBuilder("redsea", "-r", sampleRateKHz + "k").start();
        os = redsea.getOutputStream();
        task = ThreadUtils.submit(() -> {
            try {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(redsea.getInputStream(), StandardCharsets.UTF_8))) {
                    while (redsea.isAlive()) {
                        String line = reader.readLine();
                        if (line == null) break;
                        try {
                            JsonObject root = JsonParser.parseString(line).getAsJsonObject();
                            String ps = null;
                            String radiotext = null;
                            if (root.has("ps")) {
                                ps = root.get("ps").getAsString();
                            }
                            if (root.has("radiotext")) {
                                radiotext = root.get("radiotext").getAsString();
                            }
                            if (ps != null || radiotext != null) {
                                generator.provide(new AudioAnnotation(ps, radiotext));
                            }
                        } catch (Exception e) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws IOException {
        if (!isStarted()) throw new IllegalStateException("Already stopped");
        task.cancel(true);
        os.close();
        redsea.destroyForcibly();
    }

    public void writeData(byte[] data, int len) throws IOException {
        os.write(data, 0, len);
    }

}
