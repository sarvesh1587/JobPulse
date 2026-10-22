package com.jobpulse.exception;

/** 404. Also used when a resource exists but belongs to someone else, so ids aren't leaked. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
