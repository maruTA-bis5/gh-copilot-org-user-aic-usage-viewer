package io.github.marutabis5.copilotviewer.service;

/**
 * Thrown when a user-supplied input (login, year-month) fails validation.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
