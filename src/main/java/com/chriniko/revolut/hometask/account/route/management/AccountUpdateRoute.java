package com.chriniko.revolut.hometask.account.route.management;

import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.account.route.AccountRouteDefinition;
import com.chriniko.revolut.hometask.account.service.AccountService;
import com.chriniko.revolut.hometask.error.ProcessingException;
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
public class AccountUpdateRoute extends AccountRouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountUpdateRoute(@UtcZone Clock clock,
                                 ObjectMapper objectMapper,
                                 AccountService accountService,
                                 ValidatorFactory validatorFactory) {
        super(clock, objectMapper, validatorFactory);
        this.accountService = accountService;
    }

    @Override
    public String httpMethod() {
        return Methods.PUT_STRING;
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

            x.getRequestReceiver().receiveFullBytes(
                    (_exchange, message) -> {
                        ModifyAccountRequest request = deserialize(message, ModifyAccountRequest.class);
                        validateRequest(request);

                        Long optimisticReadStamp = extractEtagValue(_exchange);

                        accountService.update(accountId, request, optimisticReadStamp);
                        _exchange.setStatusCode(StatusCodes.OK);

                    }, (_exchange, e) -> {
                        throw new ProcessingException("error occurred during bytes reception", e);
                    }
            );
        });

        return provideErrorHandling(handler);
    }
}
