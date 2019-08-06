package com.chriniko.revolut.hometask.http;

import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.error.*;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import lombok.extern.log4j.Log4j2;

import javax.validation.ConstraintViolation;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Log4j2
public abstract class RouteDefinition {

    protected final Clock clock;
    protected final ObjectMapper objectMapper;
    protected final ValidatorFactory validatorFactory;


    protected RouteDefinition(Clock clock, ObjectMapper objectMapper, ValidatorFactory validatorFactory) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.validatorFactory = validatorFactory;
    }

    public abstract String httpMethod();

    public abstract String url();

    public abstract ExceptionHandler httpHandler();

    public ExceptionHandler addFallbackErrorHandler(ExceptionHandler handler) {
        return handler.addExceptionHandler(Exception.class, exchange -> {
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

    // --- internals ---

    protected <T> void validateRequest(T request) {
        Set<ConstraintViolation<T>> violations = validatorFactory.getValidator().validate(request);
        if (!violations.isEmpty()) {

            List<String> violationMessages = violations
                    .stream()
                    .map(violation -> violation.getPropertyPath().toString() + " -> " + violation.getMessage())
                    .collect(Collectors.toList());

            throw new ValidationException(violationMessages);
        }
    }

    protected HttpHandler httpHandler(Consumer<HttpServerExchange> httpServerExchangeConsumer) {

        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) {
                if (isIoTask() && exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                }

                httpServerExchangeConsumer.accept(exchange);
            }
        };
    }

    protected boolean isIoTask() {
        return false;
    }

    protected String extractRequiredParam(HttpServerExchange exchange, String param) {
        return Optional
                .ofNullable(
                        exchange.getQueryParameters().get(param)
                )
                .map(Deque::getFirst)
                .orElseThrow(() -> new ValidationException("not provided " + param));
    }

    protected Optional<String> extractParam(HttpServerExchange exchange, String param) {
        return Optional
                .ofNullable(
                        exchange.getQueryParameters().get(param)
                )
                .map(Deque::getFirst);
    }

    protected long convertToLong(String idAsString) {
        try {
            return Long.parseLong(idAsString);
        } catch (NumberFormatException e) {
            throw new ValidationException("id should be an integer");
        }
    }

    protected String serialize(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            throw new ValidationException("received malformed payload", e);
        }
    }

    protected <T> T deserialize(byte[] input, Class<T> clazz) {
        try {
            return objectMapper.readValue(input, clazz);
        } catch (JsonMappingException | JsonParseException e) {
            throw new ValidationException("received malformed payload", e);
        } catch (IOException e) {
            throw new ProcessingException("could not deserialize payload, internal error", e);
        }
    }

    protected Long extractEtagValue(HttpServerExchange exchange) {

        HeaderValues etagValue = exchange.getRequestHeaders().get(Headers.ETAG);

        return Optional.ofNullable(etagValue)
                .map(HeaderValues::getFirst)
                .map(Long::parseLong)
                .orElse(null);
    }

    protected ExceptionHandler provideErrorHandling(HttpHandler httpHandler) {

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
                            ex.getMessages(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                    exchange.getResponseSender().send(payload);
                })
                .addExceptionHandler(EntityModifiedSinceLastViewException.class, exchange -> {

                    EntityModifiedSinceLastViewException ex = (EntityModifiedSinceLastViewException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error(ex.getMessage());

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            ex.getMessage(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.PRECONDITION_FAILED);
                    exchange.getResponseSender().send(payload);
                })
                .addExceptionHandler(AcquireEntityLockFailureException.class, exchange -> {

                    AcquireEntityLockFailureException ex = (AcquireEntityLockFailureException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error(ex.getMessage());

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            ex.getMessage(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
                    exchange.getResponseSender().send(payload);
                });
    }

}
