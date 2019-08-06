package com.chriniko.revolut.hometask.health;

import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.Methods;
import lombok.extern.log4j.Log4j2;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.ValidatorFactory;
import java.time.Clock;

@Log4j2

@Dependent
public class HealthRoute extends RouteDefinition {

    @Inject
    public HealthRoute(@UtcZone Clock clock, ObjectMapper objectMapper, ValidatorFactory validatorFactory) {
        super(clock, objectMapper, validatorFactory);
    }

    @Override
    public String httpMethod() {
        return Methods.GET_STRING;
    }

    @Override
    public String url() {
        return "/health";
    }

    @Override
    public ExceptionHandler httpHandler() {

        HttpHandler handler = httpHandler(x -> {
            HealthResponse healthResponse = new HealthResponse(HealthResponse.HealthStatus.OK, "service is up and running");
            String message = serialize(healthResponse);
            x.getResponseSender().send(message);
        });

        return provideErrorHandling(handler);
    }

}
