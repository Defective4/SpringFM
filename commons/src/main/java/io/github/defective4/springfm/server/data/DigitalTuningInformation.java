package io.github.defective4.springfm.server.data;

import java.util.Collections;
import java.util.List;

public class DigitalTuningInformation {
    private final List<String> stations;

    public DigitalTuningInformation(List<String> stations) {
        this.stations = stations;
    }

    public List<String> getStations() {
        return Collections.unmodifiableList(stations);
    }

    @Override
    public String toString() {
        return "DigitalTuningInformation [stations=" + stations + "]";
    }

}
