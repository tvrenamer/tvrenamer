package com.google.code.tvrenamer.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.tvrenamer.model.UserPreferences;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class XMLPersistence {
	private static Logger logger = Logger.getLogger(XMLPersistence.class.getName());
	
	private static final XStream xstream = new XStream(new DomDriver());

	static {
		xstream.alias("preferences", UserPreferences.class);
	}

	/**
	 * Save the preferences object to the file.
	 * @param prefs the preferences object to save
	 * @param file the file to save it to
	 */
	public static void persist(UserPreferences prefs, File file) {
		String xml = xstream.toXML(prefs);
		BufferedWriter writer = null;
		
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(xml);
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Exception occoured when writing preferences file", e);
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Exception occoured when closing preferences file", e);
			}
		}
	}

	/**
	 * Load the preferences from file.
	 * @param file the file to read
	 * @return the populated preferences object
	 */
	public static UserPreferences retrieve(File file) {		
		UserPreferences preferences = null;
		
		try {
			preferences = (UserPreferences) xstream.fromXML(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			// If file doesn't exist, assume defaults
			logger.log(Level.FINER, "Preferences file '" + file.getAbsolutePath() + "' does not exist - assuming defaults");
			preferences = new UserPreferences();
		}
		
		return preferences;
	}
}
