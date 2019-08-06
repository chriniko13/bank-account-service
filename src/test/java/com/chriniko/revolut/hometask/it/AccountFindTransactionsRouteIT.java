package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.account.dto.ModifyBalanceRequest;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
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

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.StampedLock;

import static org.junit.Assert.assertEquals;

public class AccountFindTransactionsRouteIT extends SpecificationIT implements AccountGenerator, CdiHelper {

    private Faker faker;

    @Before
    public void setup() {
        accountRepository.clear();
        faker = new Faker();
    }

    @Test
    public void account_find_transactions_normal_case() throws Exception {

        // given
        long accountId = 1L;

        createAccount(1000D, accountId);

        //debit_section
        BigDecimal debitAmount = BigDecimal.valueOf(100D);

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(debitAmount);

        RequestBody debitAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request debitAccountRequest
                = new Request.Builder()
                .patch(debitAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/debit").build();


        Response debitAccountResponse = httpClient.newCall(debitAccountRequest).execute();
        assertEquals(200, debitAccountResponse.code());

        //credit_section
        BigDecimal creditAmount = BigDecimal.valueOf(12000);

        modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();

        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();
        assertEquals(200, creditAccountResponse.code());


        // when
        Request getTransactionsOfAccountRequest = new Request.Builder()
                .get()
                .url(baseUrl + "/accounts/" + accountId + "/transactions")
                .build();
        Response getTransactionsOfAccountResponse = httpClient.newCall(getTransactionsOfAccountRequest).execute();


        // then
        assertEquals(200, getTransactionsOfAccountResponse.code());

        String accountTransactions = getTransactionsOfAccountResponse.body().string();
        String expected = FileSupport.read("test/response/find/account_find_txs_normal_case.json");

        JSONAssert.assertEquals(expected, accountTransactions,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("transactions[*].timestamp", (o1, o2) -> true)
                )
        );

        getTransactionsOfAccountResponse.close();
    }

    @Test
    public void account_find_transactions_with_read_lock_normal_case() throws Exception {
        // given
        long accountId = 1L;

        createAccount(1000D, accountId);

        //debit_section
        BigDecimal debitAmount = BigDecimal.valueOf(100D);

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(debitAmount);

        RequestBody debitAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request debitAccountRequest
                = new Request.Builder()
                .patch(debitAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/debit").build();


        Response debitAccountResponse = httpClient.newCall(debitAccountRequest).execute();
        assertEquals(200, debitAccountResponse.code());

        //credit_section
        BigDecimal creditAmount = BigDecimal.valueOf(12000);

        modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();

        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();
        assertEquals(200, creditAccountResponse.code());


        // when
        Request getTransactionsOfAccountRequest = new Request.Builder()
                .get()
                .url(baseUrl + "/accounts/" + accountId + "/transactions?read-lock=enabled")
                .build();
        Response getTransactionsOfAccountResponse = httpClient.newCall(getTransactionsOfAccountRequest).execute();


        // then
        assertEquals(200, getTransactionsOfAccountResponse.code());

        String accountTransactions = getTransactionsOfAccountResponse.body().string();
        String expected = FileSupport.read("test/response/find/account_find_txs_normal_case.json");

        JSONAssert.assertEquals(expected, accountTransactions,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("transactions[*].timestamp", (o1, o2) -> true)
                )
        );

        getTransactionsOfAccountResponse.close();
    }

    @Test
    public void account_find_transactions_with_read_lock_failure_case() throws Exception {

        // given
        long accountId = 1L;

        createAccount(1000D, accountId);

        //debit_section
        BigDecimal debitAmount = BigDecimal.valueOf(100D);

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(debitAmount);

        RequestBody debitAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request debitAccountRequest
                = new Request.Builder()
                .patch(debitAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/debit").build();


        Response debitAccountResponse = httpClient.newCall(debitAccountRequest).execute();
        assertEquals(200, debitAccountResponse.code());

        //credit_section
        BigDecimal creditAmount = BigDecimal.valueOf(12000);

        modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();

        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();
        assertEquals(200, creditAccountResponse.code());


        // when
        final CountDownLatch slowOperationAcquiredWriteLockLatch = new CountDownLatch(1);

        Thread slowOperationOnAccount = new Thread(() -> {

            AccountRepository accountRepositoryNotProxied = getNonProxiedObject(accountRepository);

            ConcurrentHashMap<Long, StampedLock> stampedLocksById
                    = Reflect.on(accountRepositoryNotProxied).field("stampedLocksById").get();

            StampedLock stampedLock = stampedLocksById.get(accountId);
            long stamp = stampedLock.writeLock();
            try {
                System.out.println("....slow operation on account with id: " + accountId);
                slowOperationAcquiredWriteLockLatch.countDown();

                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ignored) {
                }

            } finally {
                stampedLock.unlock(stamp);
            }
        });
        slowOperationOnAccount.start();

        slowOperationAcquiredWriteLockLatch.await();


        Request getTransactionsOfAccountRequest = new Request.Builder()
                .get()
                .url(baseUrl + "/accounts/" + accountId + "/transactions?read-lock=enabled")
                .build();
        Response getTransactionsOfAccountResponse = httpClient.newCall(getTransactionsOfAccountRequest).execute();


        // then
        assertEquals(503, getTransactionsOfAccountResponse.code());

        String responsePayload = getTransactionsOfAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_finds_txs_read_lock_failure.json");

        JSONAssert.assertEquals(expected, responsePayload, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));


        getTransactionsOfAccountResponse.close();

    }

    @Test
    public void account_find_transactions_account_not_exists_case() throws Exception {


        // when
        Request getTransactionsOfAccountRequest = new Request.Builder()
                .get()
                .url(baseUrl + "/accounts/" + 1234 + "/transactions")
                .build();
        Response getTransactionsOfAccountResponse = httpClient.newCall(getTransactionsOfAccountRequest).execute();


        // then
        assertEquals(404, getTransactionsOfAccountResponse.code());

        String responsePayload = getTransactionsOfAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_find_txs_not_exists_account_case.json");

        JSONAssert.assertEquals(expected, responsePayload,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );

        getTransactionsOfAccountResponse.close();

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

}
