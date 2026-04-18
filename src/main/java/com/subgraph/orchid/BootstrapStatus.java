package com.subgraph.orchid;

import org.jetbrains.annotations.NotNull;

public enum BootstrapStatus {
    STARTING(0, "Starting"),
    CONN_DIR(5, "Connecting to directory server"),
    HANDSHAKE_DIR(10, "Finishing handshake with directory server"),
    ONEHOP_CREATE(15, "Establishing an encrypted directory connection"),
    REQUESTING_STATUS(20, "Asking for networkstatus consensus"),
    LOADING_STATUS(25, "Loading networkstatus consensus"),
    REQUESTING_KEYS(35, "Asking for authority key certs"),
    LOADING_KEYS(40, "Loading authority key certs"),
    REQUESTING_DESCRIPTORS(45, "Asking for relay descriptors"),
    LOADING_DESCRIPTORS(50, "Loading relay descriptors"),
    CONN_OR(80, "Connecting to the Tor network"),
    HANDSHAKE_OR(85, "Finished Handshake with first hop"),
    CIRCUIT_CREATE(90, "Establishing a Tor circuit"),
    DONE(100, "Done");

    private final int percent;
    private final String message;

    BootstrapStatus(int status, String message) {
        this.percent = status;
        this.message = message;
    }

    public int getPercent() {
        return percent;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public @NotNull String toString() {
        return String.format("[%d] - %s", percent, message);
    }
}