package com.subgraph.orchid.cells.enums;

import org.jetbrains.annotations.NotNull;

public enum RelayCellReason {
    MISC(1),
    RESOLVE_FAILED(2),
    CONNECT_REFUSED(3),
    EXIT_POLICY(4),
    DESTROY(5),
    DONE(6),
    TIMEOUT(7),
    NO_ROUTE(8),
    HIBERNATING(9),
    INTERNAL(10),
    RESOURCE_LIMIT(11),
    CONNRESET(12),
    TOR_PROTOCOL(13),
    NOT_DIRECTORY(14);

    public static @NotNull RelayCellReason ofReason(int reason) {
        for (RelayCellReason rsn : values()) {
            if (rsn.reason == reason) {
                return rsn;
            }
        }
        throw new IllegalArgumentException("Unknown RelayCell reason: " + reason);
    }

    private final int reason;

    RelayCellReason(int reason) {
        this.reason = reason;
    }

    public int getReason() {
        return reason;
    }
}