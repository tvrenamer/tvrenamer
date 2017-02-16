package org.tvrenamer.controller;

import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;

import java.util.Observable;
import java.util.Observer;
import java.util.logging.Logger;

public class UserPreferencesChangeListener implements Observer {
    private static Logger logger = Logger.getLogger(UserPreferencesChangeListener.class.getName());

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(Observable observable, Object value) {
        logger.info("Preference change event: " + value);

        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            UserPreference upref = (UserPreference) value;

            if (upref == UserPreference.PROXY) {
                // There may be incorrect entries in ShowStore if there is no internet, so clear on proxy change
                ShowStore.clear();
            }
        }
    }
}
