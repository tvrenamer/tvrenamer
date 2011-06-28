package com.google.code.tvrenamer.model;

public enum FutureResult {
	SUCCESS, EXCEPTION;
	
	private Throwable cause;
	
	public void setCause(Throwable cause) {
		this.cause = cause;
	}
	
	public Throwable getCause() {
		return cause;
	}
}
