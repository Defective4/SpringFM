package io.github.defective4.springfm.server.data;

public class AnalogTuningInformation {
    private final float minFreq, maxFreq, step;

    public AnalogTuningInformation(float minFreq, float maxFreq, float step) {
        this.minFreq = minFreq;
        this.maxFreq = maxFreq;
        this.step = step;
    }

    public float getMaxFreq() {
        return maxFreq;
    }

    public float getMinFreq() {
        return minFreq;
    }

    public float getStep() {
        return step;
    }

    @Override
    public String toString() {
        return "AnalogTuningInformation [minFreq=" + minFreq + ", maxFreq=" + maxFreq + ", step=" + step + "]";
    }

}
