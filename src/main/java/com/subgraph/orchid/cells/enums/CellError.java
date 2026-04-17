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

    public static String errorToDescription(CellError cellError) {
        return switch (cellError) {
            case NONE -> "No error reason given";
            case PROTOCOL -> "Tor protocol violation";
            case INTERNAL -> "Internal error";
            case REQUESTED -> "Response to a TRUNCATE command sent from client";
            case HIBERNATING -> "Not currently operating; trying to save bandwidth.";
            case RESOURCELIMIT -> "Out of memory, sockets, or circuit IDs.";
            case CONNECTFAILED -> "Unable to reach server.";
            case OR_IDENTITY -> "Connected to server, but its OR identity was not as expected.";
            case OR_CONNECTION_CLOSED -> "The OR connection that was carrying this circuit died.";
            case FINISHED -> "The circuit has expired for being dirty or old.";
            case TIMEOUT -> "Circuit construction took too long.";
            case DESTROYED -> "The circuit was destroyed without client TRUNCATE";
            case NOSUCHSERVICE -> "Request for unknown hidden service";
        };
    }
}