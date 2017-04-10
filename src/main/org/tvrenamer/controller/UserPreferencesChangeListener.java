package org.tvrenamer.controller;

import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.view.UIStarter;

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
