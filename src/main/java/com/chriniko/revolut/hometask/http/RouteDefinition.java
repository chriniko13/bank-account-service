package com.chriniko.revolut.hometask.http;

import com.chriniko.revolut.hometask.error.EntityNotFoundException;
import com.chriniko.revolut.hometask.error.ErrorDetails;
import com.chriniko.revolut.hometask.error.ProcessingException;
import com.chriniko.revolut.hometask.error.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.StatusCodes;
import lombok.extern.log4j.Log4j2;

import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.Optional;

@Log4j2
public abstract class RouteDefinition {

    private final Clock clock;
    protected final ObjectMapper objectMapper;

    protected RouteDefinition(Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    public abstract String httpMethod();

    public abstract String url();

    public abstract HttpHandler httpHandler();

    public abstract boolean isIoTask();

    public String extractId(HttpServerExchange exchange, String pathParam) {
        return Optional
                .ofNullable(
                        exchange.getQueryParameters().get(pathParam)
                )
                .map(Deque::getFirst)
                .orElseThrow(() -> new ValidationException("not provided id"));
    }

    protected long convertToLong(String idAsString) {
        try {
            return Long.parseLong(idAsString);
        } catch (NumberFormatException e) {
            throw new ValidationException("id should be an integer");
        }
    }

    protected HttpHandler provideErrorHandling(HttpHandler httpHandler) {

        return Handlers
                .exceptionHandler(httpHandler)
                .addExceptionHandler(ProcessingException.class, exchange -> {

                    ProcessingException ex = (ProcessingException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error("processing error occurred, message: " + ex.getMessage(), ex);

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            ex.getMessage(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.getResponseSender().send(payload);
                })
                .addExceptionHandler(ValidationException.class, exchange -> {

                    ValidationException ex = (ValidationException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error("validation error occurred, message: " + ex.getMessage(), ex);

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            ex.getMessage(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                    exchange.getResponseSender().send(payload);
                })
                .addExceptionHandler(EntityNotFoundException.class, exchange -> {

                    EntityNotFoundException ex = (EntityNotFoundException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error("entity not found error occurred, message: " + ex.getMessage());

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            ex.getMessage(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.NOT_FOUND);
                    exchange.getResponseSender().send(payload);
                })
                .addExceptionHandler(Exception.class, exchange -> {
                    Exception ex = (Exception) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error("unknown error occurred, message: " + ex.getMessage(), ex);

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            "unknown error occurred",
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
                    exchange.getResponseSender().send(payload);
                });
    }
}
