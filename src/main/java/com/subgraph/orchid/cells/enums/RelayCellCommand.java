package com.subgraph.orchid.cells.enums;

public enum RelayCellCommand {
    ESTABLISH_INTRO(32),
    ESTABLISH_RENDEZVOUS(33),
    INTRODUCE1(34),
    INTRODUCE2(35),
    RENDEZVOUS1(36),
    RENDEZVOUS2(37),
    INTRO_ESTABLISHED(38),
    RENDEZVOUS_ESTABLISHED(39),
    INTRODUCE_ACK(40);

    public static RelayCellCommand ofCommand(int comand) {
        for (RelayCellCommand cmd : values()) {
            if (cmd.command == comand) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Unknown RelayCell command: " + comand);
    }

    private final int command;

    RelayCellCommand(int command) {
        this.command = command;
    }

    public int getCommand() {
        return command;
    }
}