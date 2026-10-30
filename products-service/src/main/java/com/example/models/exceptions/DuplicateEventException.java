package com.example.models.exceptions;

public class DuplicateEventException extends RuntimeException {
    public DuplicateEventException(String eventId) {
        super("Event already processed: " + eventId);
    }
}
