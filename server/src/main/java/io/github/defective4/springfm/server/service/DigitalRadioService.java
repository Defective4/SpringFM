package io.github.defective4.springfm.server.service;

import java.io.IOException;

public non-sealed interface DigitalRadioService extends RadioService {
    int getCurrentStation();

    String[] getStations();

    void tune(int stationIndex) throws IllegalArgumentException, IOException;
}
