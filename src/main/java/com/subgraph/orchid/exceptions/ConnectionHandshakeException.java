package com.subgraph.orchid.exceptions;

public class ConnectionHandshakeException extends ConnectionIOException {
	public ConnectionHandshakeException() {
	}

	public ConnectionHandshakeException(String message) {
		super(message);
	}

	public ConnectionHandshakeException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionHandshakeException(Throwable cause) {
		super(cause);
	}

	public ConnectionHandshakeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}