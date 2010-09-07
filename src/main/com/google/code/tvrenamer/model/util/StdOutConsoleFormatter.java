package com.google.code.tvrenamer.model.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class StdOutConsoleFormatter extends Formatter {

	@Override
	public String format(LogRecord rec) {
		StringBuffer buffer = new StringBuffer(1000);

		// Date
		String formatString = "[dd/MM/yy kk:mm:ss,SS] ";
		SimpleDateFormat sdf = new SimpleDateFormat(formatString);
		Date date = new Date(rec.getMillis());
		buffer.append(sdf.format(date));

		// Level
		if (rec.getLevel() == Level.WARNING) {
			buffer.append("WARN ");
		} else {
			buffer.append(rec.getLevel() + " ");
		}

		// Class, method
		buffer.append(rec.getSourceClassName() + "#");
		buffer.append(rec.getSourceMethodName() + " ");

		// Message
		buffer.append(rec.getMessage());

		// Stacktrace
		if (rec.getThrown() != null) {
			buffer.append("\n" + rec.getThrown().getStackTrace());
		}

		// Note: No need to add a newline as that is added by the Handler

		return buffer.toString();
	}
}
