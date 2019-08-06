package com.chriniko.revolut.hometask.account.route.operation;

import com.chriniko.revolut.hometask.account.dto.FindAccountTransactionsByIdResponse;
import com.chriniko.revolut.hometask.account.dto.TransactionDto;
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
import java.util.List;

@Dependent
public class AccountFindTransactionsRoute extends AccountRouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountFindTransactionsRoute(@UtcZone Clock clock,
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
        return "/accounts/{accountId}/transactions";
    }

    @Override
    public ExceptionHandler httpHandler() {

        HttpHandler handler = httpHandler(x -> {

            Long accountId = convertToLong(extractRequiredParam(x, "accountId"));

            boolean acquireReadLock = extractParam(x, "read-lock")
                    .filter(rL -> rL.equals("enabled"))
                    .isPresent();

            Pair<Long, List<TransactionDto>> result = accountService.findTransactions(accountId, acquireReadLock);

            FindAccountTransactionsByIdResponse response = new FindAccountTransactionsByIdResponse(result.getValue1());
            String payload = serialize(response);

            x.setStatusCode(StatusCodes.OK);
            x.getResponseHeaders().add(Headers.ETAG, result.getValue0());
            x.getResponseSender().send(payload);
        });

        return provideErrorHandling(handler);
    }
}
