package com.chriniko.revolut.hometask.it.core;

public class TestInfraException extends RuntimeException {

    public TestInfraException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestInfraException(String message) {
        super(message);
    }
}
