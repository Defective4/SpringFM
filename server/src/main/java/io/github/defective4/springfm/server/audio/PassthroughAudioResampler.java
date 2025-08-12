package io.github.defective4.springfm.server.audio;

import java.io.IOException;

public class PassthroughAudioResampler implements AudioResampler {

    private final AudioSampleGenerator generator;

    public PassthroughAudioResampler(AudioSampleGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void stop() throws IOException {
    }

    @Override
    public synchronized void write(byte[] data) throws IOException {
        generator.sampleGenerated(data);
    }

}
