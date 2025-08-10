package io.github.defective4.springfm.server.packet;

public interface DataGenerator {
    void audioSampleGenerated(byte[] buffer, boolean bigEndian);

    void packetGenerated(Packet packet);
}
