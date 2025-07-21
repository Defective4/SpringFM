package io.github.defective4.springfm.server.service;

import java.io.IOException;

public interface RadioService {

    void start() throws IOException;

    void stop() throws IOException;
}
