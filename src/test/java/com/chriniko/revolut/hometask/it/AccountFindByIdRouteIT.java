package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
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
import static org.junit.Assert.assertNotNull;

public class AccountFindByIdRouteIT extends SpecificationIT implements AccountGenerator, CdiHelper {

    @Before
    public void setup() {
        accountRepository.clear();
    }

    @Test
    public void find_by_id_normal_case() throws Exception {

        // given
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());

        createAccountResponse.close();


        // when
        Request findByIdAccountRequest = new Request.Builder().url(baseUrl + "/accounts/1").build();
        Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute();


        // then
        assertEquals(200, findByIdAccountResponse.code());

        String etag = findByIdAccountResponse.header(Headers.ETAG_STRING);
        assertNotNull(etag);


        String findByIdPayload = findByIdAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_find_by_id_normal_case.json");

        JSONAssert.assertEquals(
                expected, findByIdPayload,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true)
                )
        );

        // clean up
        findByIdAccountResponse.close();
    }

    @Test
    public void find_by_id_not_exists_case() throws Exception {

        // when
        Request findByIdAccountRequest = new Request.Builder().url(baseUrl + "/accounts/1").build();
        Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute();


        // then
        assertEquals(404, findByIdAccountResponse.code());

        String actualAsString = findByIdAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_find_by_id_not_exists_case.json");

        JSONAssert.assertEquals(
                expected, actualAsString, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));


        // clean up
        findByIdAccountResponse.close();

    }

    @Test
    public void find_by_id_malformed_case() throws Exception {

        // when
        Request findByIdAccountRequest = new Request.Builder().url(baseUrl + "/accounts/foobar").build();
        Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute();


        // then
        assertEquals(400, findByIdAccountResponse.code());

        String actualAsString = findByIdAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_find_by_id_malformed_case.json");

        JSONAssert.assertEquals(
                expected, actualAsString, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));


        // clean up
        findByIdAccountResponse.close();
    }

    @Test
    public void find_by_id_read_lock_case() throws Exception {

        // given
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());

        createAccountResponse.close();


        // when
        Request findByIdAccountRequest = new Request.Builder().url(baseUrl + "/accounts/1?read-lock=enabled").build();
        Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute();


        // then
        assertEquals(200, findByIdAccountResponse.code());

        String etag = findByIdAccountResponse.header(Headers.ETAG_STRING);
        assertNotNull(etag);


        String findByIdPayload = findByIdAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_find_by_id_normal_case.json");

        JSONAssert.assertEquals(
                expected, findByIdPayload,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true)
                )
        );

        // clean up
        findByIdAccountResponse.close();

    }

    @Test
    public void find_by_id_read_lock_failure_case() throws Exception {

        // given
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());

        createAccountResponse.close();

        long accountId = 1;


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

        Request findByIdAccountRequest = new Request.Builder().url(baseUrl + "/accounts/1?read-lock=enabled").build();
        Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute();


        // then
        assertEquals(503, findByIdAccountResponse.code());

        String actual = findByIdAccountResponse.body().string();

        String expected = FileSupport.read("test/response/find/account_find_by_id_read_lock_failure_case.json");

        JSONAssert.assertEquals(
                expected, actual,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );

        // clean up
        findByIdAccountResponse.close();
    }

}
