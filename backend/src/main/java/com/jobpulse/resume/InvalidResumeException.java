package com.jobpulse.resume;

public class InvalidResumeException extends RuntimeException {
    public InvalidResumeException(String message) {
        super(message);
    }
}
