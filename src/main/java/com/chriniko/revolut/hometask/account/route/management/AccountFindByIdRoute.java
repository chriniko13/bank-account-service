package com.chriniko.revolut.hometask.account.route.management;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.route.AccountRouteDefinition;
import com.chriniko.revolut.hometask.account.service.AccountService;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.javatuples.Pair;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.validation.ValidatorFactory;
import java.time.Clock;

@Dependent
public class AccountFindByIdRoute extends AccountRouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountFindByIdRoute(@UtcZone Clock clock,
                                   ObjectMapper objectMapper,
                                   AccountService accountService,
                                   ValidatorFactory validatorFactory) {
        super(clock, objectMapper, validatorFactory);
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
    public ExceptionHandler httpHandler() {

        HttpHandler handler = httpHandler(x -> {
            String accountIdAsString = extractRequiredParam(x, "accountId");
            long accountId = convertToLong(accountIdAsString);

            boolean acquireReadLock = extractParam(x, "read-lock")
                    .filter(rL -> rL.equals("enabled"))
                    .isPresent();

            Pair<AccountDto, Long> findResult = accountService.find(accountId, acquireReadLock);

            String payload = serialize(findResult.getValue0());

            x.setStatusCode(StatusCodes.OK);
            x.getResponseHeaders().add(Headers.ETAG, findResult.getValue1());
            x.getResponseSender().send(payload);
        });

        return provideErrorHandling(handler);
    }

}
