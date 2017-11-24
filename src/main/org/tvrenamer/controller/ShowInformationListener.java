package org.tvrenamer.controller;

import org.tvrenamer.model.FailedShow;
import org.tvrenamer.model.Show;

public interface ShowInformationListener {
    void downloadSucceeded(Show show);

    void downloadFailed(FailedShow failedShow);

    void apiHasBeenDeprecated();
}
