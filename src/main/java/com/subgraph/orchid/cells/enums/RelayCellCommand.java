package com.subgraph.orchid.cells.enums;

import org.jetbrains.annotations.NotNull;

public enum RelayCellCommand {
    ESTABLISH_INTRO(32),
    ESTABLISH_RENDEZVOUS(33),
    INTRODUCE1(34),
    INTRODUCE2(35),
    RENDEZVOUS1(36),
    RENDEZVOUS2(37),
    INTRO_ESTABLISHED(38),
    RENDEZVOUS_ESTABLISHED(39),
    INTRODUCE_ACK(40),
    PADDING(0),
    BEGIN(1),
    DATA(2),
    END(3),
    CONNECTED(4),
    SENDME(5),
    EXTEND(6),
    EXTENDED(7),
    TRUNCATE(8),
    TRUNCATED(9),
    DROP(10),
    RESOLVE(11),
    RESOLVED(12),
    BEGIN_DIR(13),
    EXTEND2(14),
    EXTENDED2(15);

    public static @NotNull RelayCellCommand ofCommand(int comand) {
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