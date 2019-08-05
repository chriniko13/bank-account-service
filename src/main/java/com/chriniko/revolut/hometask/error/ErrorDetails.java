package com.chriniko.revolut.hometask.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
@Getter
public class ErrorDetails {

    private final String timestamp;
    private final String message;
    private final String details;

}
