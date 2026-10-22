package com.jobpulse.exception;

/**
 * 422: the request is well-formed but breaks a business rule, e.g. applying
 * to a closed job or moving an application from REJECTED to INTERVIEW.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
