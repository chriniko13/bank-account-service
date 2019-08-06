package com.chriniko.revolut.hometask.account.route;

import com.chriniko.revolut.hometask.account.error.AccountNotFoundException;
import com.chriniko.revolut.hometask.account.error.InsufficientFundsException;
import com.chriniko.revolut.hometask.error.ErrorDetails;
import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.StatusCodes;
import lombok.extern.log4j.Log4j2;

import javax.validation.ValidatorFactory;
import java.time.Clock;
import java.time.Instant;

@Log4j2

public abstract class AccountRouteDefinition extends RouteDefinition {

    protected AccountRouteDefinition(Clock clock, ObjectMapper objectMapper, ValidatorFactory validatorFactory) {
        super(clock, objectMapper, validatorFactory);
    }

    @Override
    protected ExceptionHandler provideErrorHandling(HttpHandler httpHandler) {
        return super
                .provideErrorHandling(httpHandler)
                .addExceptionHandler(AccountNotFoundException.class, exchange -> {

                    AccountNotFoundException ex = (AccountNotFoundException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    log.error("account not found error occurred, message: " + ex.getMessage());

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            ex.getMessage(),
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.NOT_FOUND);
                    exchange.getResponseSender().send(payload);
                })
                .addExceptionHandler(InsufficientFundsException.class, exchange -> {

                    InsufficientFundsException ex = (InsufficientFundsException) exchange.getAttachment(ExceptionHandler.THROWABLE);

                    String msg = "account has insufficient funds for debit operation";
                    log.error(msg);

                    ErrorDetails errorDetails = new ErrorDetails(
                            Instant.now(clock).toString(),
                            msg,
                            exchange.getRequestURI()
                    );

                    String payload = objectMapper.writeValueAsString(errorDetails);

                    exchange.setStatusCode(StatusCodes.BAD_REQUEST);
                    exchange.getResponseSender().send(payload);
                });
    }
}
