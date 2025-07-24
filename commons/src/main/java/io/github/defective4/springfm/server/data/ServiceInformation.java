package io.github.defective4.springfm.server.data;

public class ServiceInformation {

    public static final byte TUNING_TYPE_ANALOG = 1;
    public static final byte TUNING_TYPE_DIGITAL = 0;

    private final DigitalTuningInformation digitalTuning;
    private final int index;
    private final String name;
    private final byte tuningType;

    public ServiceInformation(int index, String name, byte tuningType, DigitalTuningInformation digitalTuning) {
        this.index = index;
        this.name = name;
        this.tuningType = tuningType;
        this.digitalTuning = digitalTuning;
    }

    public DigitalTuningInformation getDigitalTuning() {
        return digitalTuning;
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

}
