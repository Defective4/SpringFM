package io.github.defective4.springfm.server.packet;

public interface DataGenerator {
    void audioSampleGenerated(byte[] buffer);

    void packetGenerated(Packet packet);
}
