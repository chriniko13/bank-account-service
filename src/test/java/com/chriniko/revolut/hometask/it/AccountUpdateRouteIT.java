package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.AddressDto;
import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.account.dto.NameDto;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.CdiHelper;
import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import io.undertow.util.Headers;
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.StampedLock;

import static org.junit.Assert.assertEquals;

public class AccountUpdateRouteIT extends SpecificationIT implements AccountGenerator, CdiHelper {

    @Before
    public void setup() {
        accountRepository.clear();
    }

    @Test
    public void update_account_with_valid_etag_case() throws Exception {

        // given

        //create_section
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());
        createAccountResponse.close();

        //find_section
        long accountId = 1;
        Request getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        Response getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justCreatedAccountAsString = getAccountResponse.body().string();

        String expectedCreatedAccount = FileSupport.read("test/response/create/account_creation_normal_case.json");

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true)));

        String etag = getAccountResponse.header(Headers.ETAG_STRING);

        getAccountResponse.close();


        // when
        update(createAccount, 1);

        requestBody = RequestBody.create(null, objectMapper.writeValueAsBytes(createAccount));

        Request updateAccountRequest = new Request.Builder()
                .put(requestBody)
                .header(Headers.ETAG_STRING, etag)
                .url(baseUrl + "/accounts/" + accountId).build();

        Response updateAccountResponse = httpClient.newCall(updateAccountRequest).execute();


        // then
        assertEquals(200, updateAccountResponse.code());

        getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justUpdatedAccount = getAccountResponse.body().string();
        String expectedUpdatedAccount = FileSupport.read("test/response/update/account_update_with_valid_etag_case.json");

        JSONAssert.assertEquals(expectedUpdatedAccount, justUpdatedAccount,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );
    }

    @Test
    public void update_account_with_stale_etag_case() throws Exception {

        // given

        //create_section
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());
        createAccountResponse.close();

        //find_section
        long accountId = 1;
        Request getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        Response getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justCreatedAccountAsString = getAccountResponse.body().string();

        String expectedCreatedAccount = FileSupport.read("test/response/create/account_creation_normal_case.json");

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true)));

        String etag = getAccountResponse.header(Headers.ETAG_STRING);

        getAccountResponse.close();


        // when

        // but another bank account manager modifies this account....
        update(createAccount, 23);
        requestBody = RequestBody.create(null, objectMapper.writeValueAsBytes(createAccount));

        Request updateAccountRequest = new Request.Builder()
                .put(requestBody)
                .url(baseUrl + "/accounts/" + accountId).build();

        Response updateAccountResponse = httpClient.newCall(updateAccountRequest).execute();
        assertEquals(200, updateAccountResponse.code());


        // so when we will try to acquire write lock with the etag, it will fail because is stale
        update(createAccount, 1);
        requestBody = RequestBody.create(null, objectMapper.writeValueAsBytes(createAccount));
        updateAccountRequest = new Request.Builder()
                .put(requestBody)
                .header(Headers.ETAG_STRING, etag)
                .url(baseUrl + "/accounts/" + accountId).build();

        updateAccountResponse = httpClient.newCall(updateAccountRequest).execute();


        // then
        assertEquals(412, updateAccountResponse.code());

        String errorResponse = updateAccountResponse.body().string();
        String expectedErrorResponse
                = FileSupport.read("test/response/update/account_update_with_stale_etag_case.json");

        JSONAssert.assertEquals(errorResponse, expectedErrorResponse,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );

    }

    @Test
    public void update_account_with_force_write_lock_normal_case() throws Exception {

        // given

        //create_section
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());
        createAccountResponse.close();

        //find_section
        long accountId = 1;
        Request getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        Response getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justCreatedAccountAsString = getAccountResponse.body().string();

        String expectedCreatedAccount = FileSupport.read("test/response/create/account_creation_normal_case.json");

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true)));

        getAccountResponse.close();


        // when
        update(createAccount, 1);

        requestBody = RequestBody.create(null, objectMapper.writeValueAsBytes(createAccount));

        Request updateAccountRequest = new Request.Builder()
                .put(requestBody)
                .url(baseUrl + "/accounts/" + accountId).build();

        Response updateAccountResponse = httpClient.newCall(updateAccountRequest).execute();


        // then
        assertEquals(200, updateAccountResponse.code());

        getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justUpdatedAccount = getAccountResponse.body().string();
        String expectedUpdatedAccount = FileSupport.read("test/response/update/accout_update_write_lock_normal_case.json");

        JSONAssert.assertEquals(justUpdatedAccount, expectedUpdatedAccount,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );


    }

    @Test
    public void update_account_with_force_write_lock_failure_acquire_case() throws Exception {

        // given

        //create_section
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());
        createAccountResponse.close();

        //find_section
        long accountId = 1;
        Request getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        Response getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justCreatedAccountAsString = getAccountResponse.body().string();

        String expectedCreatedAccount = FileSupport.read("test/response/create/account_creation_normal_case.json");

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true)));

        getAccountResponse.close();


        // when
        final CountDownLatch slowOperationAcquiredWriteLockLatch = new CountDownLatch(1);

        Thread slowOperationOnJustCreatedAccount = new Thread(() -> {

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
        slowOperationOnJustCreatedAccount.start();

        slowOperationAcquiredWriteLockLatch.await();

        update(createAccount, 1);

        requestBody = RequestBody.create(null, objectMapper.writeValueAsBytes(createAccount));

        Request updateAccountRequest = new Request.Builder()
                .put(requestBody)
                .url(baseUrl + "/accounts/" + accountId).build();

        Response updateAccountResponse = httpClient.newCall(updateAccountRequest).execute();


        // then
        assertEquals(503, updateAccountResponse.code());

        String errorResponse = updateAccountResponse.body().string();
        String expectedErrorResponse = FileSupport.read("test/response/update/account_update_write_lock_failure_case.json");

        JSONAssert.assertEquals(errorResponse, expectedErrorResponse,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );

    }


    // --- utils ---

    private void update(ModifyAccountRequest createAccount, int time) {

        NameDto name = createAccount.getName();
        name.setFirst(name.getFirst() + " --- updated " + time);
        name.setInitials(name.getInitials() + " --- updated " + time);
        name.setLast(name.getLast() + " --- updated " + time);

        AddressDto address = createAccount.getAddress();
        address.setCity(address.getCity() + " --- updated " + time);
        address.setName(address.getName() + " --- updated " + time);
        address.setState(address.getState() + " --- updated " + time);
        address.setStreetAddress(address.getStreetAddress() + " --- updated " + time);
        address.setZipCode(address.getZipCode() + " --- updated " + time);

    }


}
