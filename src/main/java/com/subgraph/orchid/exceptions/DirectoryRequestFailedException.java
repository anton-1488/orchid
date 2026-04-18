package com.subgraph.orchid.exceptions;

public class DirectoryRequestFailedException extends TorException {
    public DirectoryRequestFailedException() {
    }

    public DirectoryRequestFailedException(String message) {
        super(message);
    }

    public DirectoryRequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public DirectoryRequestFailedException(Throwable cause) {
        super(cause);
    }

    public DirectoryRequestFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}