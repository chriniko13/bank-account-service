package com.chriniko.revolut.hometask.error;

import lombok.Getter;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Getter
public class ValidationException extends RuntimeException {

    private final List<String> messages;

    public ValidationException(String message, Throwable error) {
        super(error);
        this.messages = new LinkedList<>();
        this.messages.add(message);
    }

    public ValidationException(String message) {
        this.messages = Collections.singletonList(message);
    }

    public ValidationException(List<String> messages) {
        this.messages = new LinkedList<>(messages);
    }
}
