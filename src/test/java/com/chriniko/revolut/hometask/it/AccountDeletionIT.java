package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.CreateAccountRequest;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jboss.weld.proxy.WeldClientProxy;
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
import static org.junit.Assert.assertTrue;

public class AccountDeletionIT extends SpecificationIT implements AccountGenerator {

    @Before
    public void setup() {
        accountRepository.clear();
    }

    @Test
    public void delete_account_normal_case() throws Exception {

        // given
        CreateAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(200, createAccountResponse.code());
        createAccountResponse.close();


        // when
        Request deleteAccountByIdRequest = new Request.Builder().delete().url(baseUrl + "/accounts/1").build();
        Response deleteAccountByIdResponse = httpClient.newCall(deleteAccountByIdRequest).execute();


        // then
        assertEquals(200, deleteAccountByIdResponse.code());

        String actual = deleteAccountByIdResponse.body().string();

        String expected = FileSupport.read("test/response/account_deletion_normal_case.json");

        JSONAssert.assertEquals(
                expected, actual,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true))
        );

        assertTrue(accountRepository.findAll().isEmpty());

        // clean up
        deleteAccountByIdResponse.close();

    }

    @Test
    public void delete_account_not_exists_case() throws Exception {

        // when
        Request findByIdAccountRequest = new Request.Builder().delete().url(baseUrl + "/accounts/1").build();
        Response findByIdAccountResponse = httpClient.newCall(findByIdAccountRequest).execute();


        // then
        assertEquals(404, findByIdAccountResponse.code());

        String actualAsString = findByIdAccountResponse.body().string();

        String expected = FileSupport.read("test/response/account_deletion_not_exists_case.json");

        JSONAssert.assertEquals(
                expected, actualAsString, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));


        // clean up
        findByIdAccountResponse.close();

    }

    @Test
    public void delete_account_acquire_write_lock_failure_case() throws Exception {

        // given
        CreateAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(200, createAccountResponse.code());
        createAccountResponse.close();

        long justCreatedId = 1;

        // when
        final CountDownLatch slowOperationAcquiredWriteLockLatch = new CountDownLatch(1);

        Thread slowOperationOnJustCreatedAccount = new Thread(() -> {

            //TODO extract to method test helper...
            AccountRepository accountRepositoryNotProxied
                    = (AccountRepository) ((WeldClientProxy) accountRepository).getMetadata().getContextualInstance();

            ConcurrentHashMap<Long, StampedLock> stampedLocksById
                    = Reflect.on(accountRepositoryNotProxied).field("stampedLocksById").get();

            StampedLock stampedLock = stampedLocksById.get(justCreatedId);
            long stamp = stampedLock.writeLock();
            try {
                System.out.println("....slow operation on account with id: " + justCreatedId);
                slowOperationAcquiredWriteLockLatch.countDown();

                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException ignored) {
                }

            }finally {
                stampedLock.unlock(stamp);
            }

        });
        slowOperationOnJustCreatedAccount.start();

        slowOperationAcquiredWriteLockLatch.await();

        Request deleteAccountByIdRequest = new Request.Builder().delete().url(baseUrl + "/accounts/"+justCreatedId).build();
        Response deleteAccountByIdResponse = httpClient.newCall(deleteAccountByIdRequest).execute();


        // then
        assertEquals(200, deleteAccountByIdResponse.code());


        // clean up
        deleteAccountByIdResponse.close();
    }


    // TODO normal delete (acquire write lock) and second worker/thread tries to update but deleted...

}
