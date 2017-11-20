package org.tvrenamer.controller;

import org.tvrenamer.model.Show;

public interface ShowInformationListener {
    void downloadSucceeded(Show show);

    void downloadFailed(Show show);

    void apiHasBeenDeprecated();
}
