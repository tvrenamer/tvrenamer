package com.google.code.tvrenamer.model;

import java.io.IOException;

public class TVRenamerIOException extends IOException {
	private static final long serialVersionUID = 3028633984566046401L;

	public TVRenamerIOException(String message) {
		super(message);
	}

	public TVRenamerIOException(String message, Throwable t) {
		super(message, t);
	}
}
