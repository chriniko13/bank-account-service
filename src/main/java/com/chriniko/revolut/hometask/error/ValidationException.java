package com.chriniko.revolut.hometask.error;

public class ValidationException extends RuntimeException {
    public ValidationException(String message, Throwable error) {
        super(message, error);
    }

    public ValidationException(String message) {
        super(message);
    }
}
