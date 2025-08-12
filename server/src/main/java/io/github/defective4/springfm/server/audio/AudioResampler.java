package io.github.defective4.springfm.server.audio;

import java.io.IOException;

public interface AudioResampler {

    void start() throws IOException;

    void stop() throws IOException;

    void write(byte[] data) throws IOException;

}
