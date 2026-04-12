package com.subgraph.orchid;

public enum BootstrapStatus {
    STARTING(0),
    CONN_DIR(5),
    HANDSHAKE_DIR(10),
    ONEHOP_CREATE(15),
    REQUESTING_STATUS(20),
    LOADING_STATUS(25),
    REQUESTING_KEYS(35),
    LOADING_KEYS(40),
    REQUESTING_DESCRIPTORS(45),
    LOADING_DESCRIPTORS(50),
    CONN_OR(80),
    HANDSHAKE_OR(85),
    CIRCUIT_CREATE(90),
    DONE(100);

    private final int status;

    BootstrapStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}