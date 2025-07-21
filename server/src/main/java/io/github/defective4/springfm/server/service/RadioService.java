package io.github.defective4.springfm.server.service;

import java.io.IOException;

public interface RadioService {

    boolean isStarted();

    void start() throws IOException;

    void stop() throws IOException;
}
