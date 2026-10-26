package com.khaata.exception;

/** Thrown when a requested resource (e.g. a customer) does not exist. Maps to 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
