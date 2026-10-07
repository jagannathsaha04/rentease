package com.example.rentease.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated, e.g.
 *  - vehicle is not available   -> 409 CONFLICT
 *  - rentalDays is not positive -> 400 BAD REQUEST
 */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
