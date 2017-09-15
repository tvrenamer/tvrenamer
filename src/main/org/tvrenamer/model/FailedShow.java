package org.tvrenamer.model;

import org.tvrenamer.controller.TheTVDBProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

class FailedShow extends LocalShow {

    private final TVRenamerIOException err;

    /**
     * Find out if the results of querying for this show indicate that the API
     * is no longer supported.
     *
     * @return true if the API is deprecated; false otherwise.
     */
    public boolean isApiDeprecated() {
        return TheTVDBProvider.isApiDiscontinuedError(err);
    }

    /**
     * Log the reason for the show's failure to the given logger.
     *
     * This method does not check to see IF the show has failed.  It assumes
     * the caller has some reason to assume there was a failure, and tries
     * to provide as much information as it can.
     *
     * @param logger the logger object to send the failure message to
     */
    public void logShowFailure(Logger logger) {
        logger.log(Level.WARNING, "failed to get show for " + getName(), err);
    }

    public FailedShow(String name, TVRenamerIOException err) {
        super(name);
        this.err = err;
    }
}
