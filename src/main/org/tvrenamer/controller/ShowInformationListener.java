package org.tvrenamer.controller;

import org.tvrenamer.model.Show;

public interface ShowInformationListener {
    void downloaded(Show show);

    void downloadFailed(Show show);
}
