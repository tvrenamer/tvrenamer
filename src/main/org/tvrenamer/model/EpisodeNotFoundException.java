package org.tvrenamer.model;

public class EpisodeNotFoundException extends NotFoundException {
    private static final long serialVersionUID = 0L;

    public EpisodeNotFoundException(String message) {
        super(message);
    }
}
