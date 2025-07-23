package io.github.defective4.springfm.server.processing;

import java.io.IOException;

public interface StreamingAnnotationProcessor extends AnnotationProcessor {
    void write(byte[] data, int len) throws IOException;
}
