package io.github.defective4.springfm.backend.config.file;

import javax.sound.sampled.AudioFormat;

public class AudioFormatConfiguration {
    private final boolean bigEndian;
    private final float sampleRate;
    private final int sampleSize, channels;

    public AudioFormatConfiguration(float sampleRate, int sampleSize, int channels, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.channels = channels;
        this.bigEndian = bigEndian;
    }

    public int getChannels() {
        return channels;
    }

    public AudioFormat getFormat() {
        return new AudioFormat(sampleRate, sampleSize, channels, true, bigEndian);
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

    @Override
    public String toString() {
        return "FormatConfiguration [sampleRate=" + sampleRate + ", sampleSize=" + sampleSize + ", channels=" + channels
                + ", bigEndian=" + bigEndian + "]";
    }
}
