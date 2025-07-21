package io.github.defective4.springfm.server.processing;

import java.io.IOException;

public interface AnnotationProcessor {

    void start() throws IOException;

    void stop() throws IOException;
}
