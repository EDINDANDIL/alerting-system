package ru.models.exceptions;

public class FilterNotFoundException extends RuntimeException {
    public FilterNotFoundException(String message) {
        super(message);
    }
}
