package com.example.rentease.exception;

/** Thrown when a Customer, Vehicle or Rental does not exist -> HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
