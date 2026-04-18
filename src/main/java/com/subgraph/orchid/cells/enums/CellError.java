package com.subgraph.orchid.cells.enums;

public enum CellError {
    NONE(0),
    PROTOCOL(1),
    INTERNAL(2),
    REQUESTED(3),
    HIBERNATING(4),
    RESOURCELIMIT(5),
    CONNECTFAILED(6),
    OR_IDENTITY(7),
    OR_CONNECTION_CLOSED(8),
    FINISHED(9),
    TIMEOUT(10),
    DESTROYED(11),
    NOSUCHSERVICE(12);

    private final int error;

    CellError(int error) {
        this.error = error;
    }

    public int getError() {
        return error;
    }
}