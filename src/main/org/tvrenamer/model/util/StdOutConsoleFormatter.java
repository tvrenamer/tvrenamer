package org.tvrenamer.model.util;

import java.io.PrintWriter;
import java.io.StringWriter;
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
        String formatString = "[dd/MM/yy kk:mm:ss,SSS] ";
        SimpleDateFormat sdf = new SimpleDateFormat(formatString);
        Date date = new Date(rec.getMillis());
        buffer.append(sdf.format(date));

        // Level
        if (rec.getLevel() == Level.WARNING) {
            buffer.append("WARN ");
        } else {
            buffer.append(rec.getLevel() + " ");
        }

        // Class name (not package), method name
        buffer.append(rec.getSourceClassName().substring(rec.getSourceClassName().lastIndexOf(".") + 1) + "#");
        buffer.append(rec.getSourceMethodName() + " ");

        // Message
        buffer.append(rec.getMessage());

        // Stacktrace
        Throwable throwable = rec.getThrown();
        if (throwable != null) {
            StringWriter sink = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sink, true));
            buffer.append("\n" + sink.toString());
        }

        // Note: No need to add a newline as that is added by the Handler

        return buffer.toString();
    }
}
