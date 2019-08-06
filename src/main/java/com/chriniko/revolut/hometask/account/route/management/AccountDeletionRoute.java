package com.chriniko.revolut.hometask.account.route.management;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.route.AccountRouteDefinition;
import com.chriniko.revolut.hometask.account.service.AccountService;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.ValidatorFactory;
import java.time.Clock;

@Dependent
public class AccountDeletionRoute extends AccountRouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountDeletionRoute(@UtcZone Clock clock,
                                   ObjectMapper objectMapper,
                                   AccountService accountService,
                                   ValidatorFactory validatorFactory) {
        super(clock, objectMapper, validatorFactory);
        this.accountService = accountService;
    }

    @Override
    public String httpMethod() {
        return Methods.DELETE_STRING;
    }

    @Override
    public String url() {
        return "/accounts/{accountId}";
    }

    @Override
    public ExceptionHandler httpHandler() {
        HttpHandler handler = httpHandler(x -> {

            String accountIdAsString = extractRequiredParam(x, "accountId");
            long accountId = convertToLong(accountIdAsString);

            AccountDto deletedAccount = accountService.delete(accountId);

            String payload = serialize(deletedAccount);

            x.setStatusCode(StatusCodes.OK);
            x.getResponseSender().send(payload);
        });

        return provideErrorHandling(handler);
    }
}
