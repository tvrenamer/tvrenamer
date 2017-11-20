package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.TVRenamerIOException;
import org.tvrenamer.model.UserPreferences;

import java.util.logging.Level;
import java.util.logging.Logger;

public class UpdateChecker {
    private static final Logger logger = Logger.getLogger(UpdateChecker.class.getName());

    private static Boolean newVersionAvailable = null;
    private static String latestVersion;

    /**
     * Checks if a newer version is available.
     *
     * @return true if a new version is available, false if there is no new version or
     *          if an error has occurred
     */
    private static synchronized boolean checkIfUpdateAvailable() {
        boolean available = false;
        try {
            latestVersion = new HttpConnectionHandler().downloadUrl(TVRENAMER_VERSION_URL);
            available = latestVersion.compareToIgnoreCase(VERSION_NUMBER) > 0;
        } catch (TVRenamerIOException e) {
            // Do nothing when an exception is thrown, just don't update display
            logger.log(Level.SEVERE, "Exception when downloading version file " + TVRENAMER_VERSION_URL,
                       e);
        }

        return available;
    }

    /**
     * Checks if a newer version is available.
     *
     * @return true if a new version is available, false if there is no new version or
     *          if an error has occurred
     */
    public static synchronized boolean isUpdateAvailable() {
        if (newVersionAvailable == null) {
            newVersionAvailable = checkIfUpdateAvailable();
        }
        if (newVersionAvailable) {
            logger.info("There is a new version available, running " + VERSION_NUMBER
                        + ", new version is " + latestVersion);
            return true;
        }
        logger.finer("You have the latest version!");
        return false;
    }

    /**
     * Notifies the listener whether or not a new version is available
     *
     * @param listener
     *   the listener to update of whether or not an update is available
     */
    public static void notifyOfUpdate(final UpdateListener listener) {
        Thread updateCheckThread = new Thread(() -> {
            boolean doNotify = false;
            if (UserPreferences.getInstance().checkForUpdates()) {
                doNotify = isUpdateAvailable();
            }
            listener.notifyUpdateStatus(doNotify);
        });
        updateCheckThread.start();
    }
}
