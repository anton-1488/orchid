package com.subgraph.orchid.exceptions;


public class StreamConnectFailedException extends TorException {
    private int reason;

    public StreamConnectFailedException() {
    }

    public StreamConnectFailedException(String message, int reason) {
        super(message);
        this.reason = reason;
    }

    public StreamConnectFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamConnectFailedException(Throwable cause) {
        super(cause);
    }

    public StreamConnectFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public int getReason() {
        return reason;
    }
}