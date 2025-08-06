package io.github.defective4.springfm.server.data;

import java.util.Objects;

public class ServiceInformation {

    public static final byte TUNING_TYPE_ANALOG = 1;
    public static final byte TUNING_TYPE_DIGITAL = 0;

    private final AnalogTuningInformation analogTuning;
    private final DigitalTuningInformation digitalTuning;
    private final GainInformation gainInfo;
    private final int index;
    private final String name;
    private final byte tuningType;

    public ServiceInformation(int index, String name, byte tuningType, DigitalTuningInformation digitalTuning,
            AnalogTuningInformation analogTuning, GainInformation gainInfo) {
        this.gainInfo = Objects.requireNonNull(gainInfo);
        this.index = index;
        this.name = Objects.requireNonNull(name);
        this.tuningType = tuningType;
        this.digitalTuning = digitalTuning;
        this.analogTuning = analogTuning;
    }

    public AnalogTuningInformation getAnalogTuning() {
        return analogTuning;
    }

    public DigitalTuningInformation getDigitalTuning() {
        return digitalTuning;
    }

    public GainInformation getGainInfo() {
        return gainInfo;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public byte getTuningType() {
        return tuningType;
    }

    @Override
    public String toString() {
        return "ServiceInformation [analogTuning=" + analogTuning + ", digitalTuning=" + digitalTuning + ", gainInfo="
                + gainInfo + ", index=" + index + ", name=" + name + ", tuningType=" + tuningType + "]";
    }

}
