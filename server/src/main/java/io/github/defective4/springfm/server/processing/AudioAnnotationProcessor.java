package io.github.defective4.springfm.server.processing;

import java.io.IOException;

public interface AudioAnnotationProcessor extends AnnotationProcessor {
    void writeAudioSample(byte[] data, int len) throws IOException;
}
