package com.subgraph.orchid.exceptions;

public class OpenFailedException extends TorException {
    public OpenFailedException() {
    }

    public OpenFailedException(String message) {
        super(message);
    }

    public OpenFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpenFailedException(Throwable cause) {
        super(cause);
    }

    public OpenFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}