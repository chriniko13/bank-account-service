package com.chriniko.revolut.hometask.health;

import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import lombok.extern.log4j.Log4j2;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Clock;

@Log4j2

@Dependent
public class HealthRoute extends RouteDefinition {

    @Inject
    public HealthRoute(@UtcZone Clock clock, ObjectMapper objectMapper) {
        super(clock, objectMapper);
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
    public HttpHandler httpHandler() {
        HttpHandler handler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                // Note: dispatch to non-io threads if needed.
                if (isIoTask() && exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                }

                // Note: in worker thread
                HealthResponse healthResponse = new HealthResponse(HealthResponse.HealthStatus.OK, "service is up and running");
                String message = objectMapper.writeValueAsString(healthResponse);
                exchange.getResponseSender().send(message);
            }
        };

        return super.provideErrorHandling(handler);
    }

    @Override
    public boolean isIoTask() {
        return false;
    }

}
