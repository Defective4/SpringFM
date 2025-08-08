package io.github.defective4.springfm.backend.config.file;

import javax.sound.sampled.AudioFormat;

public class AudioFormatConfiguration {
    private final int channels;
    private final float sampleRate;

    public AudioFormatConfiguration(float sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public int getChannels() {
        return channels;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public AudioFormat toAudioFormat() {
        return new AudioFormat(sampleRate, 16, channels, true, false);
    }

    @Override
    public String toString() {
        return "AudioFormatConfiguration [sampleRate=" + sampleRate + ", channels=" + channels + "]";
    }

}
