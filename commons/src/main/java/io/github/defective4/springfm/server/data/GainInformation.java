package io.github.defective4.springfm.server.data;

public class GainInformation {
    private final boolean gainSupported;
    private final float maxGain;

    public GainInformation(float maxGain, boolean gainSupported) {
        this.maxGain = maxGain;
        this.gainSupported = gainSupported;
    }

    public float getMaxGain() {
        return maxGain;
    }

    public boolean isGainSupported() {
        return gainSupported;
    }

}
