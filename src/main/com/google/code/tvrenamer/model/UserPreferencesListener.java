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
	public void update(Observable observable, Object value) {
		logger.info("Preference change for: " + observable + ", " + value);
		
		if(observable instanceof UserPreferences && value instanceof UserPreferencesChangeEvent) {
			UserPreferences preferences = (UserPreferences) observable;
			UserPreferencesChangeEvent upce = (UserPreferencesChangeEvent) value;
			
			if(upce.getPreference().equals("moveEnabled")) {
				UIStarter.getRenameButtonText();
				UIStarter.getColumnDestText();
			} else if(upce.getPreference().equals("proxy")) {
				preferences.getProxy().apply();
			}
		}
		
	}
}
