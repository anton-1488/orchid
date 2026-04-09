package com.subgraph.orchid.exceptions;

public class ConnectionTimeoutException extends ConnectionIOException {
	public ConnectionTimeoutException() {
	}

	public ConnectionTimeoutException(String message) {
		super(message);
	}

	public ConnectionTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConnectionTimeoutException(Throwable cause) {
		super(cause);
	}

	public ConnectionTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}