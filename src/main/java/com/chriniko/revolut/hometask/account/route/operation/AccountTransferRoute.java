package com.chriniko.revolut.hometask.account.route.operation;

import com.chriniko.revolut.hometask.account.dto.TransferAmountRequest;
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
public class AccountTransferRoute extends AccountRouteDefinition {

    private final AccountService accountService;

    @Inject
    protected AccountTransferRoute(@UtcZone Clock clock,
                                   ObjectMapper objectMapper,
                                   AccountService accountService,
                                   ValidatorFactory validatorFactory) {
        super(clock, objectMapper, validatorFactory);
        this.accountService = accountService;
    }

    @Override
    public String httpMethod() {
        return Methods.POST_STRING;
    }

    @Override
    public String url() {
        return "/accounts/transfer";
    }

    @Override
    public ExceptionHandler httpHandler() {

        HttpHandler handler = httpHandler(x -> {

            x.getRequestReceiver().receiveFullBytes(
                    (_exchange, message) -> {

                        TransferAmountRequest request = deserialize(message, TransferAmountRequest.class);
                        validateRequest(request);

                        accountService.transfer(request);
                        _exchange.setStatusCode(StatusCodes.OK);

                    }, (_exchange, e) -> {
                        throw new ProcessingException("error occurred during bytes reception", e);
                    }
            );

        });

        return provideErrorHandling(handler);
    }

}
