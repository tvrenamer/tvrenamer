package org.tvrenamer.controller;

import org.tvrenamer.model.TVRenamerIOException;
import org.tvrenamer.model.util.Constants;
import org.tvrenamer.view.AboutDialog;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateChecker {
    private static Logger logger = Logger.getLogger(UpdateChecker.class.getName());

    private static final String VERSION_URL = AboutDialog.TVRENAMER_PROJECT_URL + "/version";

    /**
     * Checks if a newer version is available.
     *
     * @return the new version number as a string if available, empty string if no new version or null if an error has
     *         occurred
     */
    public static boolean isUpdateAvailable() {
        String latestVersion;
        try {
            latestVersion = new HttpConnectionHandler().downloadUrl(VERSION_URL);
        } catch (TVRenamerIOException e) {
            // Do nothing when an exception is thrown, just don't update display
            logger.log(Level.SEVERE, "Exception when downloading version file " + VERSION_URL, e);
            return false;
        }

        boolean newVersionAvailable = latestVersion.compareToIgnoreCase(Constants.VERSION_NUMBER) > 0;

        if (newVersionAvailable) {
            logger.info("There is a new version available, running " + Constants.VERSION_NUMBER + ", new version is "
                + latestVersion);
            return true;
        }
        return false;
    }
}
