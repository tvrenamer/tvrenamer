package com.google.code.tvrenamer.model.util;

public class Constants {
	public enum SWTMessageBoxType {
		OK, QUESTION, MESSAGE, WARNING, ERROR;
	}

	public enum FileCopyResult {
		SUCCESS, FAILURE;
	}

	public enum TrueFalse {
		FALSE("false", 0), TRUE("true", 1);

		private static String name;
		private static int ordinal;

		private TrueFalse(String name, int ordinal) {
			name = name;
			ordinal = ordinal;
		}

		public static String[] getAll() {
			return new String[] { FALSE.toString(), TRUE.toString() };
		}

		/**
		 * Convert the enum into an int using the ordinal value.
		 * 
		 * @return the ordinal value of the enum
		 */
		public int intValue() {
			return this.ordinal();
		}

		/**
		 * Convert the enum into a boolean.
		 * 
		 * @return the boolean value of the enum
		 */
		public boolean booleanValue() {
			return Boolean.parseBoolean(this.toString());
		}

		/**
		 * Convert an int into an enum.
		 * 
		 * @param ordinal
		 *            the int to convert
		 * @return the enum value
		 */
		public static TrueFalse enumValue(int ordinal) {
			return values()[ordinal];
		}

		@Override
		public String toString() {
			return this.name().toLowerCase();
		}
	}

	public static final String APPLICATION_NAME = "TVRenamer";

	/** The version number, this should be aligned with build.properties */
	public static final String VERSION_NUMBER = "0.5b2";

	public static final String PREFERENCES_FILE = "settings.xml";

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%e] %t";

	public static final String DEFAULT_DESTINATION_DIRECTORY = "/home/ubuntu/dev/tvrenamer/Lost";

	public static final String DEFAULT_SEASON_PREFIX = "Season ";
}
