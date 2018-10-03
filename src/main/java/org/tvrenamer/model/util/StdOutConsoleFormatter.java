package org.tvrenamer.model.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class StdOutConsoleFormatter extends Formatter {

    private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
    // note final space at end of string
    private static final String FORMAT_STRING = "[yyyy/MM/dd kk:mm:ss,SSS] ";
    // From Java's doc for DateTimeFormatter:
    //   "A formatter created from a pattern can be used as many times
    //    as necessary, it is immutable and is thread-safe."
    private static final DateTimeFormatter FORMATTER
        = DateTimeFormatter.ofPattern(FORMAT_STRING).withZone(DEFAULT_ZONE);

    @Override
    public String format(LogRecord rec) {
        StringBuilder buffer = new StringBuilder(1000);

        // Date
        Instant date = Instant.ofEpochMilli(rec.getMillis());
        buffer.append(FORMATTER.format(date));

        // Level
        if (rec.getLevel() == Level.WARNING) {
            buffer.append("WARN ");
        } else {
            buffer.append(rec.getLevel()).append(" ");
        }

        // Class name (not package), method name
        String className = rec.getSourceClassName();
        buffer.append(className.substring(className.lastIndexOf(".") + 1));
        buffer.append("#");
        buffer.append(rec.getSourceMethodName()).append(" ");

        // Message
        buffer.append(rec.getMessage());

        // Stacktrace
        //noinspection ThrowableResultOfMethodCallIgnored
        Throwable throwable = rec.getThrown();
        if (throwable != null) {
            StringWriter sink = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sink, true));
            buffer.append("\n").append(sink.toString());
        }

        // Note: No need to add a newline as that is added by the Handler
        return buffer.toString();
    }
}
