package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.account.dto.ModifyBalanceRequest;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import io.undertow.util.Headers;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class AccountCreditRouteIT extends SpecificationIT implements AccountGenerator {

    @Before
    public void setup() {
        accountRepository.clear();
    }

    @Test
    public void credit_account_normal_case() throws Exception {

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

        BigDecimal justCreatedAccountBalanceAmount = objectMapper.readValue(justCreatedAccountAsString, AccountDto.class)
                .getBalance()
                .getAmount();

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true)));

        getAccountResponse.close();

        // when
        BigDecimal creditAmount = BigDecimal.valueOf(2000.32D);

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(creditAmount, "foobar");

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();


        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();


        // then
        assertEquals(200, creditAccountResponse.code());

        getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String accountUpdatedAfterCreditOperation = getAccountResponse.body().string();
        String expectedAccountInfoAfterCreditOperation = FileSupport.read("test/response/credit/account_credit_normal_case.json");


        JSONAssert.assertEquals(expectedAccountInfoAfterCreditOperation, accountUpdatedAfterCreditOperation,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );

        BigDecimal newBalanceAmount = justCreatedAccountBalanceAmount.add(creditAmount);
        BigDecimal accountAmount = objectMapper.readValue(accountUpdatedAfterCreditOperation, AccountDto.class).getBalance().getAmount();
        assertEquals(accountAmount, newBalanceAmount);


        // clean up
        creditAccountResponse.close();
        getAccountResponse.close();

    }

    @Test
    public void credit_account_etag_provided_case() throws Exception {
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

        BigDecimal justCreatedAccountBalanceAmount = objectMapper.readValue(justCreatedAccountAsString, AccountDto.class)
                .getBalance()
                .getAmount();

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("created", (o1, o2) -> true)));

        String etag = getAccountResponse.header(Headers.ETAG_STRING);

        getAccountResponse.close();

        // when
        BigDecimal creditAmount = BigDecimal.valueOf(2000.32D);

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .header(Headers.ETAG_STRING, etag)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();


        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();


        // then
        assertEquals(200, creditAccountResponse.code());

        getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String accountUpdatedAfterCreditOperation = getAccountResponse.body().string();
        String expectedAccountInfoAfterCreditOperation = FileSupport.read("test/response/credit/account_credit_normal_case.json");


        JSONAssert.assertEquals(expectedAccountInfoAfterCreditOperation, accountUpdatedAfterCreditOperation,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );

        BigDecimal newBalanceAmount = justCreatedAccountBalanceAmount.add(creditAmount);
        BigDecimal accountAmount = objectMapper.readValue(accountUpdatedAfterCreditOperation, AccountDto.class).getBalance().getAmount();
        assertEquals(accountAmount, newBalanceAmount);


        // clean up
        creditAccountResponse.close();
        getAccountResponse.close();
    }
}
