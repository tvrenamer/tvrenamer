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

    @Override
    public String format(LogRecord rec) {
        StringBuilder buffer = new StringBuilder(1000);

        // Date
        String formatString = "[yyyy/MM/dd kk:mm:ss,SSS] ";
        ZoneId zone = ZoneId.systemDefault();
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern(formatString).withZone(zone);
        Instant date = Instant.ofEpochMilli(rec.getMillis());
        buffer.append(sdf.format(date));

        // Level
        if (rec.getLevel() == Level.WARNING) {
            buffer.append("WARN ");
        } else {
            buffer.append(rec.getLevel()).append(" ");
        }

        // Class name (not package), method name
        buffer.append(rec.getSourceClassName().substring(rec.getSourceClassName().lastIndexOf(".") + 1)).append("#");
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
