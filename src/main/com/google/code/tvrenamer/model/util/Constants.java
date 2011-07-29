package com.google.code.tvrenamer.model.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Constants {
	private static Logger logger = Logger.getLogger(Constants.class.getName());

	// Static initalisation block
	static {
		byte[] buffer = new byte[10];
		// Release env (jar)
		InputStream versionStream = Constants.class.getResourceAsStream("/tvrenamer.version");

		// Dev env
		if (versionStream == null) {
			versionStream = Constants.class.getResourceAsStream("/src/main/tvrenamer.version");
		}

		try {
			versionStream.read(buffer);
			VERSION_NUMBER = new String(buffer).trim();
		} catch (IOException e) {
			logger.log(Level.WARNING, "Exception when reading version file", e);
		}
	}

	public static final String APPLICATION_NAME = "TVRenamer";

	public static String VERSION_NUMBER;

	public static final String PREFERENCES_FILE = "tvrenamer.preferences";

	public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";

	public static final String DEFAULT_DESTINATION_DIRECTORY = System.getProperty("user.home") + "/TV";

	public static final String DEFAULT_SEASON_PREFIX = "Season ";
	
	public enum OSType { WINDOWS, LINUX, MAC };
}
