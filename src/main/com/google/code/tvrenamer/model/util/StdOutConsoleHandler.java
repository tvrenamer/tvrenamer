package com.google.code.tvrenamer.model.util;

import java.io.OutputStream;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

/**
 * 
 * @author Dave Harris
 */
public class StdOutConsoleHandler extends StreamHandler {

	private Formatter formatter = getFormatter();

	@Override
	public synchronized void publish(LogRecord logRecord) {
		// Write the formatted log record to stdout
		System.out.println(formatter.format(logRecord));
	}

	@Override
	public synchronized void close() throws SecurityException {
		// We don't want the logger to close the stdout stream
	}

	@Override
	public synchronized void flush() {
		super.flush();
	}

	@Override
	public boolean isLoggable(LogRecord record) {
		return true;
	}

	@Override
	protected synchronized void setOutputStream(OutputStream arg0) throws SecurityException {
		super.setOutputStream(arg0);
	}

}
