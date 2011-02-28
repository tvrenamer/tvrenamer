package com.google.code.tvrenamer.controller.util;

import java.util.logging.Logger;

public class StringUtils {
	private static Logger logger = Logger.getLogger(StringUtils.class.getName());

	public static String sanitiseTitle(String title) {
		// need to add more mappings, such as ':'
		title = title.replace(":", " -");
		title = title.replace('/', '-');
		title = title.replace('\\', '-');
		title = title.replace("?", "");
		title = title.replace("`", "'");
		return title;
	}

	public static String replacePunctuation(String s) {
		s = s.replaceAll("\\.", " ");
		s = s.replaceAll(",", " ");
		return s;
	}

	public static String getExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		return filename.substring(dot + 1);
	}
	
	/**
	 * Replaces unsafe HTML Characters with HTML Entities
	 * 
	 * @param input
	 *            string to encode
	 * @return HTML safe representation of input
	 */
	public static String encodeSpecialCharacters(String input) {
		if (input == null || input.length() == 0) {
			return "";
		}

		// TODO: determine other characters that need to be replaced (eg "'", "-")
		logger.finest("Input before encoding: [" + input + "]");
		input = input.replaceAll("& ", "&amp; ");
		
		// Don't encode string within xml data strings
		if(!input.startsWith("<?xml")) {
			input = input.replaceAll(" ", "%20");
		}
		logger.finest("Input after encoding: [" + input + "]");
		return input;
	}
}
