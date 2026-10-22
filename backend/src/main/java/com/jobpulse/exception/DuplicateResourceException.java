package com.jobpulse.exception;

/** 409: the request would create something that already exists (same email, same application). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
