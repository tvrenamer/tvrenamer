package com.google.code.tvrenamer.view;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;

public class UIUtils {

	private static Logger logger = Logger.getLogger(UIStarter.class.getName());
	private static Shell shell;

	/**
	 * Constructor.
	 * 
	 * @param shell
	 *            the shell to use.
	 */
	public UIUtils(Shell shell) {
		UIUtils.shell = shell;
	}

	/**
	 * Determine the system default font
	 * 
	 * @param shell
	 *            the shell to get the font from
	 * @return the system default font
	 */
	public static FontData getDefaultSystemFont() {
		FontData defaultFont = null;
		try {
			defaultFont = shell.getDisplay().getSystemFont().getFontData()[0];
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error attempting to determine system default font", e);
		}

		return defaultFont;
	}

	/**
	 * Show a message box of the given type with the given message content and window title.
	 * 
	 * @param type
	 *            the {@link SWTMessageBoxType} to create
	 * @param message
	 *            the message content
	 * @param title
	 *            the window title
	 */
	public static void showMessageBox(final SWTMessageBoxType type, final String title, final String message) {
		final int swtIconValue;

		switch (type) {
			case QUESTION:
				swtIconValue = SWT.ICON_QUESTION;
				break;
			case MESSAGE:
				swtIconValue = SWT.ICON_INFORMATION;
				break;
			case WARNING:
				swtIconValue = SWT.ICON_WARNING;
				break;
			case ERROR:
				swtIconValue = SWT.ICON_ERROR;
				break;
			case OK:
				// Intentional missing break
			default:
				swtIconValue = SWT.OK;
		}

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				MessageBox msgBox = new MessageBox(shell, swtIconValue);
				msgBox.setText(title);
				msgBox.setMessage(message);
				msgBox.open();
			}
		});
	}

	public static void handleNoConnection(Exception exception) {
		String message = "Unable connect to the TV listing website, please check your internet connection.  "
			+ "\nNote that proxies are not currently supported.";
		logger.log(Level.WARNING, message, exception);
		showMessageBox(SWTMessageBoxType.ERROR, "Error", message);
	}

	public static boolean isMac() {
		if (System.getProperty("os.name").equals("Mac OS X")) {
			return true;
		}
		return false;
	}
}
