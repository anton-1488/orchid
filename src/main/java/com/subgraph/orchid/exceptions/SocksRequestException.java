package com.subgraph.orchid.exceptions;

public class SocksRequestException extends TorException {
    public SocksRequestException() {
    }

    public SocksRequestException(String message) {
        super(message);
    }

    public SocksRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocksRequestException(Throwable cause) {
        super(cause);
    }

    public SocksRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}