package com.chriniko.revolut.hometask.it.core;

import com.chriniko.revolut.hometask.error.ProcessingException;
import com.chriniko.revolut.hometask.error.ValidationException;
import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;

import java.time.Clock;
import java.util.Deque;
import java.util.Optional;

public class ErrorHandlingDemonstrationRoute extends RouteDefinition {

    protected ErrorHandlingDemonstrationRoute() {
        super(Clock.systemUTC(), new ObjectMapper());
    }

    @Override
    public String httpMethod() {
        return Methods.GET_STRING;
    }

    @Override
    public String url() {
        return "/error-handling-demonstration";
    }

    @Override
    public HttpHandler httpHandler() {
        HttpHandler dummyHandler = exchange -> {

            String errorType = Optional
                    .ofNullable(
                            exchange.getQueryParameters().get("error-type")
                    )
                    .map(Deque::getFirst)
                    .orElse("other");
            switch (errorType) {
                case "processing":
                    throw new ProcessingException("processing error", new IllegalStateException());

                case "validation":
                    throw new ValidationException("validation error");

                case "other":
                    throw new IllegalStateException("boom");
            }

        };

        return super.provideErrorHandling(dummyHandler);
    }

    @Override
    public boolean isIoTask() {
        return false;
    }
}
