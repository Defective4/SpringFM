package io.github.defective4.springfm.server.data;

import javax.sound.sampled.AudioFormat;

public class SerializableAudioFormat {

    private final int channels;
    private final float sampleRate;
    private final int sampleSize;

    public SerializableAudioFormat(AudioFormat format) {
        this(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels());
    }

    public SerializableAudioFormat(float sampleRate, int sampleSize, int channels) {
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.channels = channels;
    }

    public int getChannels() {
        return channels;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSize() {
        return sampleSize;
    }
}
