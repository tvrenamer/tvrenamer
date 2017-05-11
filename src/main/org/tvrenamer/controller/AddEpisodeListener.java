package org.tvrenamer.controller;

import org.tvrenamer.model.FileEpisode;

import java.util.Queue;

public interface AddEpisodeListener {
    void addEpisodes(Queue<FileEpisode> episodes);
}
