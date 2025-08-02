package io.github.defective4.springfm.server.data;

import javax.sound.sampled.AudioFormat;

public class SerializableAudioFormat {
    private final boolean bigEndian;
    private final int channels;
    private final float sampleRate;
    private final int sampleSize;

    public SerializableAudioFormat(AudioFormat format) {
        this(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.isBigEndian());
    }

    public SerializableAudioFormat(float sampleRate, int sampleSize, int channels, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.channels = channels;
        this.bigEndian = bigEndian;
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

    public boolean isBigEndian() {
        return bigEndian;
    }

}
