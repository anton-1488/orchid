package com.subgraph.orchid.circuits.cells.enums;

public enum CellCommand {
    /**
     * Command constant for a PADDING type cell.
     */
    PADDING(0),

    /**
     * Command constant for a CREATE type cell.
     */
    CREATE(1),

    /**
     * Command constant for a CREATED type cell.
     */
    CREATED(2),

    /**
     * Command constant for a RELAY type cell.
     */
    RELAY(3),

    /**
     * Command constant for a DESTROY type cell.
     */
    DESTROY(4),

    /**
     * Command constant for a CREATE_FAST type cell.
     */
    CREATE_FAST(5),

    /**
     * Command constant for a CREATED_FAST type cell.
     */
    CREATED_FAST(6),

    /**
     * Command constant for a VERSIONS type cell.
     */
    VERSIONS(7),

    /**
     * Command constant for a NETINFO type cell.
     */
    NETINFO(8),

    /**
     * Command constant for a RELAY_EARLY type cell.
     */
    RELAY_EARLY(9),

    /**
     * Command constant for a VPADDING type cell.
     */
    VPADDING(128),

    /**
     * Command constant for a CERTS type cell.
     */
    CERTS(129),

    /**
     * Command constant for a AUTH_CHALLENGE type cell.
     */
    AUTH_CHALLENGE(130),

    /**
     * Command constant for a AUTHENTICATE type cell.
     */
    AUTHENTICATE(131),

    /**
     * Command constant for a AUTHORIZE type cell.
     */
    AUTHORIZE(132);

    public static CellCommand ofCommand(int comand) {
        for (CellCommand cmd : values()) {
            if (cmd.command == comand) {
                return cmd;
            }
        }
        throw new IllegalArgumentException("Unknown cell command: " + comand);
    }

    private final int command;

    CellCommand(int command) {
        this.command = command;
    }

    public int getCommand() {
        return command;
    }
}