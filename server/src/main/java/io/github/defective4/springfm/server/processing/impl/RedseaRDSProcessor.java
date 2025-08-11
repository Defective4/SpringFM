package io.github.defective4.springfm.server.processing.impl;

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
import io.github.defective4.springfm.server.processing.AudioAnnotationProcessor;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class RedseaRDSProcessor implements AudioAnnotationProcessor {

    private static final int sampleRateKHz = 171;
    private final AnnotationGenerator generator;
    private boolean lastNonMusic;
    private String lastTitle, lastText;

    private OutputStream os;

    private Process redsea;

    private Future<?> task;

    public RedseaRDSProcessor(AnnotationGenerator generator) {
        this.generator = Objects.requireNonNull(generator);
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
                            if (root.has("ps")) {
                                lastTitle = root.get("ps").getAsString();
                            }
                            if (root.has("radiotext")) {
                                lastText = root.get("radiotext").getAsString();
                            }
                            if (root.has("is_music")) {
                                lastNonMusic = !root.get("is_music").getAsBoolean();
                            }

                            if (lastText != null || lastText != null) {
                                generator.provide(new AudioAnnotation(lastTitle, lastText, lastNonMusic));
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
        lastText = null;
        lastTitle = null;
        task.cancel(true);
        os.close();
        redsea.destroyForcibly();
    }

    @Override
    public void writeAudioSample(byte[] data, int len) throws IOException {
        os.write(data, 0, len);
    }

}
