/*
 * Log.java - A class for logging events
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.gjt.sp.util;

//{{{ Imports
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;

import java.text.DateFormat;

import java.util.*;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import static java.text.DateFormat.MEDIUM;
//}}}

/**
 * This class provides methods for logging events. In terms of functionality,
 * it is somewhere in between <code>System.out.println()</code> and
 * full-blown logging packages such as log4j.<p>
 *
 * All events are logged to an in-memory buffer and optionally a stream,
 * and those with a high urgency (warnings and errors) are also printed
 * to standard output.<p>
 *
 * Logging of exception tracebacks is supported.<p>
 *
 * This class can also optionally redirect standard output and error to the log.
 *
 * @author Slava Pestov
 * @version $Id: Log.java 12789 2008-06-04 21:23:10Z kpouer $
 */
public class Log
{
	//{{{ Constants
	/**
	 * The maximum number of log messages that will be kept in memory.
	 * @since jEdit 2.6pre5
	 */
	public static final int MAXLINES = 500;

	/**
	 * Debugging message urgency. Should be used for messages only
	 * useful when debugging a problem.
	 * @since jEdit 2.2pre2
	 */
	public static final int DEBUG = 1;

	/**
	 * Message urgency. Should be used for messages which give more
	 * detail than notices.
	 * @since jEdit 2.2pre2
	 */
	public static final int MESSAGE = 3;

	/**
	 * Notice urgency. Should be used for messages that directly
	 * affect the user.
	 * @since jEdit 2.2pre2
	 */
	public static final int NOTICE = 5;

	/**
	 * Warning urgency. Should be used for messages that warrant
	 * attention.
	 * @since jEdit 2.2pre2
	 */
	public static final int WARNING = 7;

	/**
	 * Error urgency. Should be used for messages that signal a
	 * failure.
	 * @since jEdit 2.2pre2
	 */
	public static final int ERROR = 9;
	//}}}

	//{{{ init() method
	/**
	 * Initializes the log.
	 * @param stdio If true, standard output and error will be
	 * sent to the log
	 * @param level Messages with this log level or higher will
	 * be printed to the system console
	 * @since jEdit 3.2pre4
	 */
	public static void init(boolean stdio, int level)
	{
		if(stdio)
		{
			if(System.out == realOut && System.err == realErr)
			{
				System.setOut(createPrintStream(NOTICE,null));
				System.setErr(createPrintStream(ERROR,null));
			}
		}

		Log.level = level;

		// Log some stuff
		log(MESSAGE,Log.class,"When reporting bugs, please"
			+ " include the following information:");
		String[] props = {
			"java.version", "java.vm.version", "java.runtime.version",
			"java.vendor", "java.compiler", "os.name", "os.version",
			"os.arch", "user.home", "java.home",
			"java.class.path",
			};
		for(int i = 0; i < props.length; i++)
		{
			log(MESSAGE,Log.class,
				props[i] + '=' + System.getProperty(props[i]));
		}
	} //}}}

	//{{{ setLogWriter() method
	/**
	 * Writes all currently logged messages to this stream if there was no
	 * stream set previously, and sets the stream to write future log
	 * messages to.
	 * @param stream The writer
	 * @since jEdit 3.2pre4
	 */
	public static void setLogWriter(Writer stream)
	{
		if(Log.stream == null && stream != null)
		{
			try
			{
				if(wrap)
				{
					for(int i = logLineCount; i < log.length; i++)
					{
						stream.write(log[i]);
						stream.write(lineSep);
					}
				}
				for(int i = 0; i < logLineCount; i++)
				{
					stream.write(log[i]);
					stream.write(lineSep);
				}

				stream.flush();
			}
			catch(Exception e)
			{
				// do nothing, who cares
			}
		}

		Log.stream = stream;
	} //}}}

	//{{{ flushStream() method
	/**
	 * Flushes the log stream.
	 * @since jEdit 2.6pre5
	 */
	public static void flushStream()
	{
		if(stream != null)
		{
			try
			{
				stream.flush();
			}
			catch(IOException io)
			{
				io.printStackTrace(realErr);
			}
		}
	} //}}}

	//{{{ closeStream() method
	/**
	 * Closes the log stream. Should be done before your program exits.
	 * @since jEdit 2.6pre5
	 */
	public static void closeStream()
	{
		if(stream != null)
		{
			try
			{
				stream.close();
				stream = null;
			}
			catch(IOException io)
			{
				io.printStackTrace(realErr);
			}
		}
	} //}}}

	//{{{ getLogListModel() method
	/**
	 * Returns the list model for viewing the log contents.
	 * @since jEdit 4.2pre1
	 */
	public static ListModel getLogListModel()
	{
		return listModel;
	} //}}}

	//{{{ log() method
	/**
	 * Logs an exception with a message.
	 *
	 * If an exception is the cause of a call to {@link #log}, then
	 * the exception should be explicitly provided so that it can
	 * be presented to the (debugging) user in a useful manner
	 * (not just the exception message, but also the exception stack trace)
	 *
	 * @since jEdit 4.3pre5
	 */
	public static void log(int urgency, Object source, Object message,
		Throwable exception)
	{
		// We can do nicer here, but this is a start...
		log(urgency,source,message);
		log(urgency,source,exception);
	} //}}}

	//{{{ log() method
	/**
	 * Logs a message. This method is thread-safe.<p>
	 *
	 * The following code sends a typical debugging message to the activity
	 * log:
	 * <pre>Log.log(Log.DEBUG,this,"counter = " + counter);</pre>
	 * The corresponding activity log entry might read as follows:
	 * <pre>[debug] JavaParser: counter = 15</pre>
	 *
	 * @param urgency The urgency; can be one of
	 * <code>Log.DEBUG</code>, <code>Log.MESSAGE</code>,
	 * <code>Log.NOTICE</code>, <code>Log.WARNING</code>, or
	 * <code>Log.ERROR</code>.
	 * @param source The source of the message, either an object or a
	 * class instance. When writing log messages from macros, set
	 * this parameter to <code>BeanShell.class</code> to make macro
	 * errors easier to spot in the activity log.
	 * @param message The message. This can either be a string or
	 * an exception
	 *
	 * @since jEdit 2.2pre2
	 */
	public static void log(int urgency, Object source, Object message)
	{
		String _source;
		if(source == null)
		{
			_source = Thread.currentThread().getName();
			if(_source == null)
			{
				_source = Thread.currentThread().getClass().getName();
			}
		}
		else if(source instanceof Class)
			_source = ((Class)source).getName();
		else
			_source = source.getClass().getName();
		int index = _source.lastIndexOf('.');
		if(index != -1)
			_source = _source.substring(index+1);

		if(message instanceof Throwable)
		{
			_logException(urgency,source,(Throwable)message);
		}
		else
		{
			String _message = String.valueOf(message);
			// If multiple threads log stuff, we don't want
			// the output to get mixed up
			synchronized(LOCK)
			{
				StringTokenizer st = new StringTokenizer(
					_message,"\r\n");
				int lineCount = 0;
				boolean oldWrap = wrap;
				while(st.hasMoreTokens())
				{
					lineCount++;
					_log(urgency,_source,st.nextToken()
						.replace('\t',' '));
				}
				listModel.update(lineCount,oldWrap);
			}
		}
	} //}}}

	//{{{ Private members

	//{{{ Instance variables
	private static final Object LOCK;
	private static final String[] log;
	private static int logLineCount;
	private static boolean wrap;
	private static int level;
	private static Writer stream;
	private static final String lineSep;
	private static final PrintStream realOut;
	private static final PrintStream realErr;
	private static final LogListModel listModel;
	private static final DateFormat timeFormat;
	private static final int MAX_THROWABLES = 10;
	public static final List<Throwable> throwables;
	//}}}

	//{{{ Class initializer
	static
	{
		LOCK = new Object();
		level = WARNING;

		realOut = System.out;
		realErr = System.err;

		log = new String[MAXLINES];
		lineSep = System.getProperty("line.separator");
		listModel = new LogListModel();
		
		timeFormat = DateFormat.getTimeInstance(MEDIUM);
		throwables = Collections.synchronizedList(new ArrayList<Throwable>(MAX_THROWABLES));
	} //}}}

	//{{{ createPrintStream() method
	private static PrintStream createPrintStream(final int urgency,
		final Object source)
	{
		return new LogPrintStream(urgency, source);
	} //}}}

	//{{{ _logException() method
	private static void _logException(final int urgency,
		final Object source,
		final Throwable message)
	{
		PrintStream out = createPrintStream(urgency,source);
		if (urgency >= level)
		{
			synchronized (throwables)
			{
				if (throwables.size() == MAX_THROWABLES)
				{
					throwables.remove(0);
				}
				throwables.add(message);
			}
		}
		synchronized(LOCK)
		{
			message.printStackTrace(out);
		}
	} //}}}

	//{{{ _log() method
	private static void _log(int urgency, String source, String message)
	{
		String fullMessage = timeFormat.format(new Date()) + " ["+Thread.currentThread().getName()+"] [" + urgencyToString(urgency) + "] " + source
			+ ": " + message;

		try
		{
			log[logLineCount] = fullMessage;
			if(++logLineCount >= log.length)
			{
				wrap = true;
				logLineCount = 0;
			}

			if(stream != null)
			{
				stream.write(fullMessage);
				stream.write(lineSep);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(realErr);
		}

		if(urgency >= level)
		{
			if(urgency == ERROR)
				realErr.println(fullMessage);
			else
				realOut.println(fullMessage);
		}
	} //}}}

	//{{{ urgencyToString() method
	private static String urgencyToString(int urgency)
	{
		switch(urgency)
		{
		case DEBUG:
			return "debug";
		case MESSAGE:
			return "message";
		case NOTICE:
			return "notice";
		case WARNING:
			return "warning";
		case ERROR:
			return "error";
		}

		throw new IllegalArgumentException("Invalid urgency: " + urgency);
	} //}}}

	//}}}

	//{{{ LogListModel class
	static class LogListModel implements ListModel
	{
		final List<ListDataListener> listeners = new ArrayList<ListDataListener>();

		//{{{ fireIntervalAdded() method
		private void fireIntervalAdded(int index1, int index2)
		{
			for(int i = 0; i < listeners.size(); i++)
			{
				ListDataListener listener = listeners.get(i);
				listener.intervalAdded(new ListDataEvent(this,
					ListDataEvent.INTERVAL_ADDED,
					index1,index2));
			}
		} //}}}

		//{{{ fireIntervalRemoved() method
		private void fireIntervalRemoved(int index1, int index2)
		{
			for(int i = 0; i < listeners.size(); i++)
			{
				ListDataListener listener = listeners.get(i);
				listener.intervalRemoved(new ListDataEvent(this,
					ListDataEvent.INTERVAL_REMOVED,
					index1,index2));
			}
		} //}}}

		//{{{ addListDataListener() method
		public void addListDataListener(ListDataListener listener)
		{
			listeners.add(listener);
		} //}}}

		//{{{ removeListDataListener() method
		public void removeListDataListener(ListDataListener listener)
		{
			listeners.remove(listener);
		} //}}}

		//{{{ getElementAt() method
		public Object getElementAt(int index)
		{
			if(wrap)
			{
				if(index < MAXLINES - logLineCount)
					return log[index + logLineCount];
				else
					return log[index - MAXLINES + logLineCount];
			}
			else
				return log[index];
		} //}}}

		//{{{ getSize() method
		public int getSize()
		{
			if(wrap)
				return MAXLINES;
			else
				return logLineCount;
		} //}}}

		//{{{ update() method
		void update(final int lineCount, final boolean oldWrap)
		{
			if(lineCount == 0 || listeners.isEmpty())
				return;

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if(wrap)
					{
						if(oldWrap)
							fireIntervalRemoved(0,lineCount - 1);
						else
						{
							fireIntervalRemoved(0,
								logLineCount);
						}
						fireIntervalAdded(
							MAXLINES - lineCount + 1,
							MAXLINES);
					}
					else
					{
						fireIntervalAdded(
							logLineCount - lineCount + 1,
							logLineCount);
					}
				}
			});
		} //}}}
	} //}}}

	//{{{ LogPrintStream class
	/**
	 * A print stream that uses the "Log" class to output the messages,
	 * and has special treatment for the printf() function. Using this
	 * stream has one caveat: printing messages that don't have a line
	 * break at the end will have one added automatically...
	 */
	private static class LogPrintStream extends PrintStream {

		private final ByteArrayOutputStream buffer;
		private final OutputStream orig;

		//{{{ LogPrintStream constructor
		LogPrintStream(int urgency, Object source)
		{
			super(new LogOutputStream(urgency, source));
			buffer = new ByteArrayOutputStream();
			orig = out;
		} //}}}

		//{{{ printf() method
		/**
		 * This is a hack to allow "printf" to not print weird
		 * stuff to the output. Since "printf" doesn't seem to
		 * print the whole message in one shot, our output
		 * stream above would break a line of log into several
		 * lines; so we buffer the result of the printf call and
		 * print the whole thing in one shot. A similar hack
		 * would be needed for the "other" printf method, but
		 * I'll settle for the common case only.
		 */
		public PrintStream printf(String format, Object... args)
		{
			synchronized (orig)
			{
				buffer.reset();
				out = buffer;
				super.printf(format, args);

				try
				{
					byte[] data = buffer.toByteArray();
					orig.write(data, 0, data.length);
					out = orig;
				}
				catch (IOException ioe)
				{
					// don't do anything?
				}
				finally
				{
					buffer.reset();
				}
			}
			return this;
		} //}}}
	} //}}}

	//{{{ LogOutputStream class
	private static class LogOutputStream extends OutputStream
	{
		private final int 	urgency;
		private final Object 	source;

		//{{{ LogOutputStream constructor
		LogOutputStream(int urgency, Object source)
		{
			this.urgency 	= urgency;
			this.source 	= source;
		} //}}}

		//{{{ write() method
		public synchronized void write(int b)
		{
			byte[] barray = { (byte)b };
			write(barray,0,1);
		} //}}}

		//{{{ write() method
		public synchronized void write(byte[] b, int off, int len)
		{
			String str = new String(b,off,len);
			log(urgency,source,str);
		} //}}}
	} //}}}
}
