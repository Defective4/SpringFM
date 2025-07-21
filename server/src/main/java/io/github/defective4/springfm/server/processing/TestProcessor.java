package io.github.defective4.springfm.server.processing;

import java.io.IOException;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.data.AnnotationProvider;
import io.github.defective4.springfm.server.data.AudioAnnotation;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class TestProcessor implements AnnotationProcessor {
    private final AnnotationProvider provideer;
    private Future<?> task;

    public TestProcessor(AnnotationProvider provider) {
        provideer = provider;
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
                provideer.provide(new AudioAnnotation("RMF FM", "Testowy opis"));
            }
        });
    }

    @Override
    public void stop() throws IOException {
        task.cancel(true);
    }
}
