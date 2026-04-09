package com.subgraph.orchid.exceptions;

public class PathSelectionFailedException extends TorException {
	public PathSelectionFailedException() {
	}

	public PathSelectionFailedException(String message) {
		super(message);
	}

	public PathSelectionFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public PathSelectionFailedException(Throwable cause) {
		super(cause);
	}

	public PathSelectionFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
