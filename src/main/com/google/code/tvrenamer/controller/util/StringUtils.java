package com.google.code.tvrenamer.controller.util;

public class StringUtils {

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

}
