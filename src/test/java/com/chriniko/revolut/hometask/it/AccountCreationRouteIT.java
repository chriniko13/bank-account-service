package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.dto.CreateAccountRequest;
import com.chriniko.revolut.hometask.account.dto.FindAllAccountsResponse;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import com.chriniko.revolut.hometask.it.core.TestInfraException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
import lombok.Getter;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class AccountCreationRouteIT extends SpecificationIT implements AccountGenerator {

    @Before
    public void setup() {
        accountRepository.clear();
    }

    @Test
    public void create_account_normal_case() throws Exception {

        // given
        CreateAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        // when
        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        // then
        assertEquals(200, createAccountResponse.code());

        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        assertEquals(200, findAllAccountsResponse.code());

        FindAllAccountsResponse allAccountsResponse = objectMapper.readValue(
                findAllAccountsResponse.body().string(),
                FindAllAccountsResponse.class
        );
        List<AccountDto> accounts = allAccountsResponse.getAccounts();

        assertEquals(1, accounts.size());


        //clean up
        createAccountResponse.close();
        findAllAccountsResponse.close();
    }

    @Test
    public void create_account_concurrent_clients_case() throws Exception {

        // given
        int clients = 200;
        int accountsCreationPerClient = 3;

        // when
        MultipleClientsOperation multipleClientsOperation
                = new MultipleClientsOperation(clients, accountsCreationPerClient).invoke();

        boolean reachedZero = multipleClientsOperation.getLatch().await(25, TimeUnit.SECONDS);
        if (!reachedZero) {
            throw new IllegalStateException("multiple clients operation not successful");
        }


        // then
        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();

        assertEquals(200, findAllAccountsResponse.code());

        FindAllAccountsResponse allAccountsResponse = objectMapper.readValue(
                findAllAccountsResponse.body().string(),
                FindAllAccountsResponse.class
        );
        List<AccountDto> accounts = allAccountsResponse.getAccounts();

        Awaitility.await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals(clients * accountsCreationPerClient, accounts.size());
        });


        // clear
        ForkJoinPool pool = multipleClientsOperation.getPool();
        pool.shutdown();
    }


    // --- utils ---

    @Getter
    private class MultipleClientsOperation {

        private final int clients;
        private final int accountsCreationPerClient;
        private final Faker faker;
        private final CountDownLatch latch;
        private final ForkJoinPool pool;
        private final Runnable task;

        MultipleClientsOperation(int clients, int accountsCreationPerClient) {

            this.clients = clients;
            this.accountsCreationPerClient = accountsCreationPerClient;
            this.faker = new Faker();

            this.latch = new CountDownLatch(clients);
            this.pool = new ForkJoinPool();

            this.task = () -> {

                CreateAccountRequest sampleAccountRequest = createSampleAccountRequest(faker);
                String payload = null;
                try {
                    payload = objectMapper.writeValueAsString(sampleAccountRequest);
                } catch (JsonProcessingException e) {
                    throw new TestInfraException("could not serialize request", e);
                }

                RequestBody requestBody = RequestBody.create(null, payload);
                Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

                for (int i = 1; i <= accountsCreationPerClient; i++) {
                    try {
                        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();
                        assertEquals(200, createAccountResponse.code());
                        createAccountResponse.close();

                    } catch (IOException e) {
                        throw new TestInfraException("could not perform test task", e);
                    }
                }

                latch.countDown();
            };

        }

        MultipleClientsOperation invoke() {
            for (int i = 1; i <= clients; i++) {
                pool.submit(task);
            }
            return this;
        }
    }

}
