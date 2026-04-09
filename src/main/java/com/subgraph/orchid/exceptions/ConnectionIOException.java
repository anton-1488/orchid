package com.subgraph.orchid.exceptions;

public class ConnectionIOException extends TorException {
	public ConnectionIOException() {
	}

	public ConnectionIOException(String message) {
		super(message);
	}

	public ConnectionIOException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionIOException(Throwable cause) {
		super(cause);
	}

	public ConnectionIOException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}