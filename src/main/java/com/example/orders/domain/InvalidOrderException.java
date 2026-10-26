package com.example.orders.domain;

/** The input violates a business rule. Knows nothing about HTTP status codes. */
public class InvalidOrderException extends RuntimeException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
