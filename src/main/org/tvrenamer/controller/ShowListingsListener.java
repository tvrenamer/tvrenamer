package org.tvrenamer.controller;

import org.tvrenamer.model.Show;

public interface ShowListingsListener {
    void downloadListingsComplete(Show show);

    void downloadListingsFailed(Show show);
}
