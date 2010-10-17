package com.google.code.tvrenamer.model.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;
import com.google.code.tvrenamer.view.UIUtils;

public class UpdateChecker {
	
	private static Logger logger = Logger.getLogger(UpdateChecker.class.getName());

	/**
	 * Checks if a newer version is available.
	 * @return the new version number as a string if available, empty string if no new version or null if an error has occurred
	 */
	public static Boolean isUpdateAvailable() {
		URL versionFileUrl = null;
		
		try {
			versionFileUrl = new URL("http://r.ac.nz/tvrenamer.version");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(versionFileUrl.openStream()));
			String latestVersion = reader.readLine();
			
			if (latestVersion.compareToIgnoreCase(Constants.VERSION_NUMBER) > 0) {
				return true;
			}
		} catch (Exception e) {
			UIUtils.showMessageBox(SWTMessageBoxType.ERROR, "Error", "There was a problem checking for updates.\n\n" +
					"Please make sure you are connected to the internet or try again later.");
			logger.log(Level.WARNING, "Exception thrown when downloading or parsing tvrenamer version file at " + versionFileUrl, e);
			return null;
		}

		return false;
	}
}
