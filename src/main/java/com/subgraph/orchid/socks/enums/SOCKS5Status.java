package com.subgraph.orchid.socks.enums;

public enum SOCKS5Status {
    SUCCESS(0),
    FAILURE(1),
    CONNECTION_REFUSED(5),
    COMMAND_NOT_SUPPORTED(7);


    private final int status;

    SOCKS5Status(int status) {
        this.status = status;
    }

    public static SOCKS5Status ofStatus(int stt) {
        for (SOCKS5Status status : values()) {
            if (status.status == stt) {
                return status;
            }
        }
        throw new EnumConstantNotPresentException(SOCKS5Status.class, String.valueOf(stt));
    }

    public int getStatus() {
        return status;
    }
}