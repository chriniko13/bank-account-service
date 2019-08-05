package com.chriniko.revolut.hometask.account.route;

import com.chriniko.revolut.hometask.account.dto.CreateAccountRequest;
import com.chriniko.revolut.hometask.account.service.AccountService;
import com.chriniko.revolut.hometask.error.ProcessingException;
import com.chriniko.revolut.hometask.error.ValidationException;
import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;
import java.time.Clock;

@Dependent
public class AccountCreationRoute extends RouteDefinition {

    private final AccountService accountService;

    @Inject
    public AccountCreationRoute(@UtcZone Clock clock, ObjectMapper objectMapper, AccountService accountService) {
        super(clock, objectMapper);
        this.accountService = accountService;
    }

    @Override
    public String httpMethod() {
        return Methods.POST_STRING;
    }

    @Override
    public String url() {
        return "/accounts";
    }

    @Override
    public HttpHandler httpHandler() {

        HttpHandler handler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) {
                if (isIoTask() && exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                }

                exchange.getRequestReceiver().receiveFullBytes(
                        (_exchange, message) -> {
                            try {
                                CreateAccountRequest createAccountRequest = objectMapper.readValue(message, CreateAccountRequest.class);
                                //createAccountRequest.validate(); TODO

                                accountService.process(createAccountRequest);
                                exchange.setStatusCode(StatusCodes.OK);

                            } catch (JsonMappingException | JsonParseException e) {
                                throw new ValidationException("received malformed payload", e);
                            } catch (IOException e) {
                                throw new ProcessingException("could not deserialize payload, internal error", e);
                            }

                        }, (_exchange, e) -> {
                            throw new ProcessingException("error occurred during bytes reception", e);
                        }
                );

            }
        };

        return super.provideErrorHandling(handler);
    }

    @Override
    public boolean isIoTask() {
        return false;
    }
}
