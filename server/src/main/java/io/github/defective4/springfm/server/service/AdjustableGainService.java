package io.github.defective4.springfm.server.service;

import java.io.IOException;

public interface AdjustableGainService {
    float getCurrentGain();

    float getMaxGain();

    void setGain(float gain) throws IOException, IllegalArgumentException;
}
