package com.jobpulse.ingestion;

public class JobSourceFetchException extends RuntimeException {
    public JobSourceFetchException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobSourceFetchException(String message) {
        super(message);
    }
}
