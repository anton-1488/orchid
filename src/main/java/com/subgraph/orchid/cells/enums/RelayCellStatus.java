package com.subgraph.orchid.cells.enums;

public enum RelayCellStatus {
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

    public static RelayCellStatus ofCStatus(int status) {
        for (RelayCellStatus stt : values()) {
            if (stt.status == status) {
                return stt;
            }
        }
        throw new IllegalArgumentException("Unknown RelayCellStatus status: " + status);
    }

    private final int status;

    RelayCellStatus(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}