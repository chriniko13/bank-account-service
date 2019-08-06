package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.*;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.error.ErrorDetails;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.CdiHelper;
import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import com.github.javafaker.Faker;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.StampedLock;

import static org.junit.Assert.assertEquals;

public class AccountTransferRouteIT extends SpecificationIT implements AccountGenerator, CdiHelper {

    private Faker faker;

    @Before
    public void setup() {
        accountRepository.clear();
        faker = new Faker();
    }

    @Test
    public void transfer_money_normal_case() throws Exception {

        // given
        double firstAccountAmount = 1000.0D;
        double secondAccountAmount = 1500.0D;
        double transferAmount = 500;

        createAccount(firstAccountAmount, 1);

        createAccount(secondAccountAmount, 2);

        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        FindAllAccountsResponse findAllAccountsResponseDto
                = objectMapper.readValue(findAllAccountsResponse.body().string(), FindAllAccountsResponse.class);

        assertEquals(2, findAllAccountsResponseDto.getAccounts().size());

        findAllAccountsResponse.close();

        BigDecimal firstAccountAmountBefore = getAccount(1).getBalance().getAmount();
        BigDecimal secondAccountAmountBefore = getAccount(2).getBalance().getAmount();


        // when
        TransferAmountRequest transferAmountRequestDto = new TransferAmountRequest(
                1,
                2,
                BigDecimal.valueOf(transferAmount),
                null
        );
        String transferAmountRequestDtoPayload = objectMapper.writeValueAsString(transferAmountRequestDto);

        Request transferRequest = new Request.Builder()
                .url(baseUrl + "/accounts/transfer")
                .post(RequestBody.create(null, transferAmountRequestDtoPayload))
                .build();

        Response transferRequestResponse = httpClient.newCall(transferRequest).execute();


        // then
        assertEquals(200, transferRequestResponse.code());
        transferRequestResponse.close();


        BigDecimal firstAccountAmountAfter = getAccount(1).getBalance().getAmount();
        BigDecimal secondAccountAmountAfter = getAccount(2).getBalance().getAmount();

        assertEquals(firstAccountAmountBefore.subtract(BigDecimal.valueOf(transferAmount)), firstAccountAmountAfter);
        assertEquals(secondAccountAmountBefore.add(BigDecimal.valueOf(transferAmount)), secondAccountAmountAfter);


        Request getTransactionsOfAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/1/transactions").build();
        Response getTransactionsOfAccountResponse = httpClient.newCall(getTransactionsOfAccountRequest).execute();

        String firstAccountTransactions = getTransactionsOfAccountResponse.body().string();
        String firstAccountTransactionsExpected = FileSupport.read("test/response/transfer/transfer_first_account_txs_state_normal_case.json");
        JSONAssert.assertEquals(firstAccountTransactionsExpected, firstAccountTransactions,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("transactions[*].timestamp", (o1, o2) -> true)
                )
        );

        getTransactionsOfAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/2/transactions").build();
        getTransactionsOfAccountResponse = httpClient.newCall(getTransactionsOfAccountRequest).execute();

        String secondAccountTransactions = getTransactionsOfAccountResponse.body().string();
        String secondAccountTransactionsExpected = FileSupport.read("test/response/transfer/transfer_second_account_txs_state_normal_case.json");
        JSONAssert.assertEquals(secondAccountTransactionsExpected, secondAccountTransactions,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("transactions[*].timestamp", (o1, o2) -> true)
                )
        );

    }

    @Test
    public void insufficient_funds_source_account_case() throws Exception {

        // given

        double firstAccountAmount = 1000.0D;
        double secondAccountAmount = 1500.0D;
        double transferAmount = 1000.01;

        createAccount(firstAccountAmount, 1);

        createAccount(secondAccountAmount, 2);


        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        FindAllAccountsResponse findAllAccountsResponseDto
                = objectMapper.readValue(findAllAccountsResponse.body().string(), FindAllAccountsResponse.class);

        assertEquals(2, findAllAccountsResponseDto.getAccounts().size());

        findAllAccountsResponse.close();


        // when
        TransferAmountRequest transferAmountRequestDto = new TransferAmountRequest(
                1,
                2,
                BigDecimal.valueOf(transferAmount),
                null
        );
        String transferAmountRequestDtoPayload = objectMapper.writeValueAsString(transferAmountRequestDto);

        Request transferRequest = new Request.Builder()
                .url(baseUrl + "/accounts/transfer")
                .post(RequestBody.create(null, transferAmountRequestDtoPayload))
                .build();

        Response transferRequestResponse = httpClient.newCall(transferRequest).execute();


        // then
        assertEquals(400, transferRequestResponse.code());


        String actual = transferRequestResponse.body().string();
        String expected = FileSupport.read("test/response/transfer/transfer_insufficient_funds_case.json");

        JSONAssert.assertEquals(
                expected, actual,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );


        transferRequestResponse.close();
    }

    @Test
    public void transfer_money_source_account_write_lock_acquired_case() throws Exception {

        // given
        double firstAccountAmount = 1000.0D;
        double secondAccountAmount = 1500.0D;
        double transferAmount = 500;

        createAccount(firstAccountAmount, 1);

        createAccount(secondAccountAmount, 2);

        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        FindAllAccountsResponse findAllAccountsResponseDto
                = objectMapper.readValue(findAllAccountsResponse.body().string(), FindAllAccountsResponse.class);

        assertEquals(2, findAllAccountsResponseDto.getAccounts().size());

        findAllAccountsResponse.close();


        // when
        final CountDownLatch slowOperationAcquiredWriteLockLatch = new CountDownLatch(1);

        Thread slowOperationOnSourceAccount = new Thread(() -> {

            AccountRepository accountRepositoryNotProxied = getNonProxiedObject(accountRepository);

            ConcurrentHashMap<Long, StampedLock> stampedLocksById
                    = Reflect.on(accountRepositoryNotProxied).field("stampedLocksById").get();

            StampedLock stampedLock = stampedLocksById.get(1L);
            long stamp = stampedLock.writeLock();
            try {
                System.out.println("....slow operation on account with id: " + 1);
                slowOperationAcquiredWriteLockLatch.countDown();

                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ignored) {
                }

            } finally {
                stampedLock.unlock(stamp);
            }
        });
        slowOperationOnSourceAccount.start();

        slowOperationAcquiredWriteLockLatch.await();

        TransferAmountRequest transferAmountRequestDto = new TransferAmountRequest(
                1,
                2,
                BigDecimal.valueOf(transferAmount),
                null
        );
        String transferAmountRequestDtoPayload = objectMapper.writeValueAsString(transferAmountRequestDto);

        Request transferRequest = new Request.Builder()
                .url(baseUrl + "/accounts/transfer")
                .post(RequestBody.create(null, transferAmountRequestDtoPayload))
                .build();

        Response transferRequestResponse = httpClient.newCall(transferRequest).execute();


        // then
        assertEquals(503, transferRequestResponse.code());

        String responsePayload = transferRequestResponse.body().string();
        String expected = FileSupport.read("test/response/transfer/transfer_source_account_write_lock_acquired_failure.json");

        JSONAssert.assertEquals(
                expected, responsePayload,
                new CustomComparator(JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );

        transferRequestResponse.close();
    }

    @Test
    public void transfer_money_destination_account_write_lock_acquired_case() throws Exception {

        // given
        double firstAccountAmount = 1000.0D;
        double secondAccountAmount = 1500.0D;
        double transferAmount = 500;

        createAccount(firstAccountAmount, 1);

        createAccount(secondAccountAmount, 2);

        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        FindAllAccountsResponse findAllAccountsResponseDto
                = objectMapper.readValue(findAllAccountsResponse.body().string(), FindAllAccountsResponse.class);

        assertEquals(2, findAllAccountsResponseDto.getAccounts().size());

        findAllAccountsResponse.close();


        // when
        final CountDownLatch slowOperationAcquiredWriteLockLatch = new CountDownLatch(1);

        Thread slowOperationOnSourceAccount = new Thread(() -> {

            AccountRepository accountRepositoryNotProxied = getNonProxiedObject(accountRepository);

            ConcurrentHashMap<Long, StampedLock> stampedLocksById
                    = Reflect.on(accountRepositoryNotProxied).field("stampedLocksById").get();

            StampedLock stampedLock = stampedLocksById.get(2L);
            long stamp = stampedLock.writeLock();
            try {
                System.out.println("....slow operation on account with id: " + 1);
                slowOperationAcquiredWriteLockLatch.countDown();

                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ignored) {
                }

            } finally {
                stampedLock.unlock(stamp);
            }
        });
        slowOperationOnSourceAccount.start();

        slowOperationAcquiredWriteLockLatch.await();

        TransferAmountRequest transferAmountRequestDto = new TransferAmountRequest(
                1,
                2,
                BigDecimal.valueOf(transferAmount),
                null
        );
        String transferAmountRequestDtoPayload = objectMapper.writeValueAsString(transferAmountRequestDto);

        Request transferRequest = new Request.Builder()
                .url(baseUrl + "/accounts/transfer")
                .post(RequestBody.create(null, transferAmountRequestDtoPayload))
                .build();

        Response transferRequestResponse = httpClient.newCall(transferRequest).execute();


        // then
        assertEquals(503, transferRequestResponse.code());

        String responsePayload = transferRequestResponse.body().string();
        String expected = FileSupport.read("test/response/transfer/transfer_dest_account_write_lock_acquired_failure.json");

        JSONAssert.assertEquals(
                expected, responsePayload,
                new CustomComparator(JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );

        transferRequestResponse.close();
    }

    @Test
    public void transfer_money_validation_case() throws Exception {
        // given
        double firstAccountAmount = 1000.0D;
        double secondAccountAmount = 1500.0D;
        double transferAmount = -500;

        createAccount(firstAccountAmount, 1);
        createAccount(secondAccountAmount, 2);

        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        FindAllAccountsResponse findAllAccountsResponseDto
                = objectMapper.readValue(findAllAccountsResponse.body().string(), FindAllAccountsResponse.class);

        assertEquals(2, findAllAccountsResponseDto.getAccounts().size());
        findAllAccountsResponse.close();

        // when
        TransferAmountRequest transferAmountRequestDto = new TransferAmountRequest(
                1,
                2,
                BigDecimal.valueOf(transferAmount),
                null
        );
        String transferAmountRequestDtoPayload = objectMapper.writeValueAsString(transferAmountRequestDto);

        Request transferRequest = new Request.Builder()
                .url(baseUrl + "/accounts/transfer")
                .post(RequestBody.create(null, transferAmountRequestDtoPayload))
                .build();

        Response transferRequestResponse = httpClient.newCall(transferRequest).execute();


        // then
        assertEquals(400, transferRequestResponse.code());

        String actual = transferRequestResponse.body().string();

        ErrorDetails errorDetails = objectMapper.readValue(actual, ErrorDetails.class);
        List<String> messages = errorDetails.getMessages();

        assertEquals(
                new HashSet<>(Collections.singletonList("amount -> must be greater than 0")),
                new HashSet<>(messages)
        );

        transferRequestResponse.close();
    }

    // --- utils ---

    private void createAccount(double creditAmount, long accountId) throws Exception {

        ModifyAccountRequest createAccount = createSampleAccountRequest(faker);
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        try (Response createAccountResponse = httpClient.newCall(createAccountRequest).execute()) {
            assertEquals(201, createAccountResponse.code());
        }

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);
        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();

        try (Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute()) {
            assertEquals(200, creditAccountResponse.code());
        }

    }

    private AccountDto getAccount(long accountId) throws IOException {
        Request findByIdAccountRequest = new Request.Builder().url(baseUrl + "/accounts/" + accountId).build();
        try (Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute()) {
            return objectMapper.readValue(findByIdAccountResponse.body().string(), AccountDto.class);
        }
    }


}
