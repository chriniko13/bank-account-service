package com.chriniko.revolut.hometask.error;

public class ProcessingException extends RuntimeException {

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
