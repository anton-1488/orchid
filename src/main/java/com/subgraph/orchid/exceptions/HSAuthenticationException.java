package com.subgraph.orchid.exceptions;

public class HSAuthenticationException extends TorException {
	public HSAuthenticationException() {
	}

	public HSAuthenticationException(String message) {
		super(message);
	}

	public HSAuthenticationException(String message, Throwable cause) {
		super(message, cause);
	}

	public HSAuthenticationException(Throwable cause) {
		super(cause);
	}

	public HSAuthenticationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}