package io.github.defective4.springfm.server.packet;

@FunctionalInterface
public interface PacketGenerator {
    void packetGenerated(Packet packet);
}
