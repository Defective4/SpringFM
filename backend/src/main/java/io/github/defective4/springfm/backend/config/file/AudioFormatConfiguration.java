package io.github.defective4.springfm.backend.config.file;

import javax.sound.sampled.AudioFormat;

public class AudioFormatConfiguration {
    private final float sampleRate;
    private final int sampleSize, channels;

    public AudioFormatConfiguration(float sampleRate, int sampleSize, int channels) {
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

    public AudioFormat toAudioFormat() {
        return new AudioFormat(sampleRate, sampleSize, channels, true, false);
    }

    @Override
    public String toString() {
        return "AudioFormatConfiguration [sampleRate=" + sampleRate + ", sampleSize=" + sampleSize + ", channels="
                + channels + "]";
    }
}
