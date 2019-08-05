package com.chriniko.revolut.hometask.account.route;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.service.AccountService;
import com.chriniko.revolut.hometask.http.RouteDefinition;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.javatuples.Pair;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.Clock;

@Dependent
public class AccountFindByIdRoute extends RouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountFindByIdRoute(@UtcZone Clock clock,
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
        return "/accounts/{accountId}";
    }

    @Override
    public HttpHandler httpHandler() {
        HttpHandler handler = exchange -> {

            String accountIdAsString = extractId(exchange, "accountId");
            long accountId = convertToLong(accountIdAsString);

            Pair<AccountDto, Long> findResult = accountService.find(accountId);

            String payload = objectMapper.writeValueAsString(findResult.getValue0());

            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseHeaders().add(Headers.ETAG, findResult.getValue1());
            exchange.getResponseSender().send(payload);
        };

        return super.provideErrorHandling(handler);
    }

    @Override
    public boolean isIoTask() {
        return false;
    }
}
