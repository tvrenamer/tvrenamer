package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.TVRenamerIOException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateChecker {
    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());

    /**
     * Checks if a newer version is available.
     *
     * @return the new version number as a string if available, empty string if no new version
     *          or null if an error has occurred
     */
    public static boolean isUpdateAvailable() {
        String latestVersion;
        try {
            latestVersion = new HttpConnectionHandler().downloadUrl(TVRENAMER_VERSION_URL);
        } catch (TVRenamerIOException e) {
            // Do nothing when an exception is thrown, just don't update display
            logger.log(Level.SEVERE, "Exception when downloading version file " + TVRENAMER_VERSION_URL, e);
            return false;
        }

        boolean newVersionAvailable = latestVersion.compareToIgnoreCase(VERSION_NUMBER) > 0;

        if (newVersionAvailable) {
            logger.info("There is a new version available, running " + VERSION_NUMBER + ", new version is "
                + latestVersion);
            return true;
        }
        return false;
    }
}
