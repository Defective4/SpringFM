package io.github.defective4.springfm.server.data;

public class PlayerCommand {

    public static final byte COMMAND_ANALOG_TUNE = 3;
    public static final byte COMMAND_CHANGE_SERVICE = 1;
    public static final byte COMMAND_DIGITAL_TUNE = 2;

    private final byte command;
    private final String data;

    public PlayerCommand(byte command, String data) {
        this.command = command;
        this.data = data;
    }

    public byte getCommand() {
        return command;
    }

    public String getData() {
        return data;
    }

}
