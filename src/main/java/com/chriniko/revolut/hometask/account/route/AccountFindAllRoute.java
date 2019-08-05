package com.chriniko.revolut.hometask.account.route;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.dto.FindAllAccountsResponse;
import com.chriniko.revolut.hometask.account.service.AccountService;
import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Clock;
import java.util.List;


@Dependent
public class AccountFindAllRoute extends RouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountFindAllRoute(@UtcZone Clock clock,
                                  ObjectMapper objectMapper,
                                  AccountService accountService) {
        super(clock, objectMapper);
        this.accountService = accountService;
    }

    @Override
    public String httpMethod() {
        return Methods.GET_STRING;
    }

    @Override
    public String url() {
        return "/accounts";
    }

    @Override
    public HttpHandler httpHandler() {

        HttpHandler handler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                if (isIoTask() && exchange.isInIoThread()) {
                    exchange.dispatch(this);
                    return;
                }

                List<AccountDto> accounts = accountService.findAll();
                FindAllAccountsResponse response = new FindAllAccountsResponse(accounts);

                String payload = objectMapper.writeValueAsString(response);

                exchange.setStatusCode(StatusCodes.OK);
                exchange.getResponseSender().send(payload);
            }
        };

        return super.provideErrorHandling(handler);
    }

    @Override
    public boolean isIoTask() {
        return false;
    }
}
