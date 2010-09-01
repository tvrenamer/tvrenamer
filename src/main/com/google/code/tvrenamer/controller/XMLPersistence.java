package com.google.code.tvrenamer.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.google.code.tvrenamer.model.TVRenamerIOException;
import com.google.code.tvrenamer.model.UserPreferences;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XMLPersistence {
	private static final XStream xstream = new XStream(new DomDriver());

	static {
		xstream.alias("preferences", UserPreferences.class);
	}

	public static void persist(UserPreferences prefs, File file) throws IOException {
		String xml = xstream.toXML(prefs);
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(xml);
		writer.close();
	}

	public static UserPreferences retrieve(File file) throws IOException {
		if (!file.exists()) {
			throw new TVRenamerIOException("Preferences file '" + file.getAbsolutePath() + "' does not exist!");
		}
		return (UserPreferences) xstream.fromXML(new FileInputStream(file));
	}
}
