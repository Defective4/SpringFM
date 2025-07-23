package io.github.defective4.springfm.server.packet.impl;

import io.github.defective4.springfm.server.data.PlayerCommand;
import io.github.defective4.springfm.server.packet.PacketPayload;

public class PlayerCommandPayload extends PacketPayload {

    public PlayerCommandPayload(PlayerCommand command) {
        super("command", command);
    }

}
