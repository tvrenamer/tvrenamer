package org.tvrenamer.controller;

public interface ShowListingsListener {
    void listingsDownloadComplete();

    void listingsDownloadFailed(Exception err);
}
