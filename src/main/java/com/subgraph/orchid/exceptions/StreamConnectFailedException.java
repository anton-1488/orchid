package com.subgraph.orchid.exceptions;


public class StreamConnectFailedException extends TorException {
    public StreamConnectFailedException() {
    }

    public StreamConnectFailedException(String message) {
        super(message);
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
}