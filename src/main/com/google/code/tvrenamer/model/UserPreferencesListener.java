package com.google.code.tvrenamer.model;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import com.google.code.tvrenamer.view.UIStarter;

public class UserPreferencesListener implements Observer {
	private static Logger logger = Logger.getLogger(UserPreferencesListener.class.getName());

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable newPreferences, Object newValue) {
		logger.info("Preference change for " + newPreferences + ", changing to " + newValue);
		
		UIStarter.getRenameButtonText();
	}
}
