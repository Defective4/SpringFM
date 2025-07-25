package io.github.defective4.springfm.server.service;

import java.io.IOException;

public non-sealed interface AnalogRadioService extends RadioService {
    float getCurrentFrequency();

    float getFrequencyStep();

    float getMaxFrequency();

    float getMinFrequency();

    void tune(float freq) throws IllegalArgumentException, IOException;
}
