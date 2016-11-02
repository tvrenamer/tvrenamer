package org.tvrenamer.model;

public class NotFoundException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    public NotFoundException(String message) {
        super(message);
    }
}
