package org.tvrenamer.controller;

import org.tvrenamer.model.Show;

public interface ShowListingsListener {
    void listingsDownloadComplete(Show show);

    void listingsDownloadFailed(Show show);
}
