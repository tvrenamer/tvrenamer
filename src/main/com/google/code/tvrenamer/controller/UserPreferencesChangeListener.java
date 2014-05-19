package com.google.code.tvrenamer.controller;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

import com.google.code.tvrenamer.model.ShowStore;
import com.google.code.tvrenamer.model.UserPreferences;
import com.google.code.tvrenamer.view.UIStarter;

public class UserPreferencesChangeListener implements Observer {
	private static Logger logger = Logger.getLogger(UserPreferencesChangeListener.class.getName());

	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable observable, Object value) {
		logger.info("Preference change event: " + value);

		if (observable instanceof UserPreferences && value instanceof UserPreferencesChangeEvent) {
			UserPreferencesChangeEvent upce = (UserPreferencesChangeEvent) value;

			if (upce.getPreference().equals("moveEnabled")) {
				UIStarter.setRenameButtonText();
				UIStarter.setColumnDestText();
			}

			if (upce.getPreference().equals("proxy")) {
				// There may be incorrect entries in ShowStore if there is no internet, so clear on proxy change
				ShowStore.clear();
			}
		}
	}
}
