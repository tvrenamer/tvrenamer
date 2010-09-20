package com.google.code.tvrenamer.view;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;

public class UIUtils {

	private static Logger logger = Logger.getLogger(UIStarter.class.getName());

	/**
	 * Determine the system default font
	 * 
	 * @param shell
	 *            the shell to get the font from
	 * @return the name of the system default font
	 */
	public static String getDefaultSystemFont(Shell shell) {
		String fontName = "";
		try {
			fontName = shell.getDisplay().getSystemFont().getFontData()[0].getName();
		} catch (Exception e) {
			logger.log(Level.WARNING, "Error attempting to determine system default font", e);
		}

		return fontName;
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
	public static void showMessageBox(final Shell shell, final SWTMessageBoxType type, final String title,
		final String message) {
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
			@Override
			public void run() {
				MessageBox msgBox = new MessageBox(shell, swtIconValue);
				msgBox.setText(title);
				msgBox.setMessage(message);
				msgBox.open();
			}
		});
	}
}
