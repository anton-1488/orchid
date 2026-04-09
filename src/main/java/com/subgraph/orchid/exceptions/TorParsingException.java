package com.subgraph.orchid.exceptions;


public class TorParsingException extends TorException {
	public TorParsingException() {
	}

	public TorParsingException(String message) {
		super(message);
	}

	public TorParsingException(String message, Throwable cause) {
		super(message, cause);
	}

	public TorParsingException(Throwable cause) {
		super(cause);
	}

	public TorParsingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}