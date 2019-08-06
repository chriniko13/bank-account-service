package com.chriniko.revolut.hometask.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class ErrorDetails {

    private final String timestamp;

    private final String message;
    private final List<String> messages;

    private final String details;


    public ErrorDetails(String timestamp, String message, String details) {
        this.timestamp = timestamp;
        this.message = message;
        this.messages = null;
        this.details = details;
    }

    public ErrorDetails(String timestamp, List<String> messages, String details) {
        this.timestamp = timestamp;
        this.message = null;
        this.messages = messages;
        this.details = details;
    }

    // Note: used by jackson.
    public ErrorDetails() {
        this.timestamp = null;
        this.message = null;
        this.messages = null;
        this.details = null;
    }

}
