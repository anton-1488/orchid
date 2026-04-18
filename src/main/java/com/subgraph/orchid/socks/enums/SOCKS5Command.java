package com.subgraph.orchid.socks.enums;

import org.jetbrains.annotations.NotNull;

public enum SOCKS5Command {
    VERSION(5),
    AUTH_NONE(0),
    NO_ACCEPTABLE(0xFF),
    COMMAND_CONNECT(1);

    private final int command;

    SOCKS5Command(int command) {
        this.command = command;
    }

    public static @NotNull SOCKS5Command ofCommand(int cmd) {
        for (SOCKS5Command command : values()) {
            if (command.command == cmd) {
                return command;
            }
        }
        throw new EnumConstantNotPresentException(SOCKS5Command.class, String.valueOf(cmd));
    }

    public int getCommand() {
        return command;
    }
}