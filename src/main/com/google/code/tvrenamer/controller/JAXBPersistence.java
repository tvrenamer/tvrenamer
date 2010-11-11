package com.google.code.tvrenamer.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.google.code.tvrenamer.model.UserPreferences;

public class JAXBPersistence {
	private static Logger logger = Logger.getLogger(JAXBPersistence.class.getName());
	private static JAXBContext context;
	
	static {
		try {
			context = JAXBContext.newInstance(UserPreferences.class);
		} catch (JAXBException e) {
			logger.log(Level.SEVERE, "Unable to create JAXB Context for UserPreferences class", e);
		}
	}

	/**
	 * Marshals a {@link UserPreferences} instance to an xml file
	 * @param prefs the {@link UserPreferences} instance
	 * @param file the xml config file
	 * @throws IOException
	 * @throws JAXBException
	 */
	public static void persist(UserPreferences prefs, File file) {
		try {
			Marshaller marshaller = context.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(prefs, file);
		} catch (JAXBException e) {
			logger.log(Level.SEVERE, "Unable to marshal UserPreferences " + prefs.toString() + " to xml file " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * Unmarshals the xml configuration file to a {@link UserPreferences} instance
	 * @param file the xml config file
	 * @return the {@link UserPreferences} instance
	 */
	public static UserPreferences retrieve(File file) {
		try {
			Unmarshaller unmarshaller = context.createUnmarshaller();
			return (UserPreferences) unmarshaller.unmarshal(file);
		} catch (JAXBException jaxbe) {
			// If it can't find the settings file, just create an UserPreferences instance
			if(jaxbe.getLinkedException() instanceof FileNotFoundException) {
    			try {
    				return new UserPreferences();
    			} catch (IOException ioe) {
    				logger.log(Level.WARNING, "Failed to create default preferences", ioe);
    			}
			} else {
				logger.log(Level.SEVERE, "Unable from unmarshal xml file " + file.getAbsolutePath() + " to UserPreferences", jaxbe);
			}
		}
		
		return null;
	}
}
