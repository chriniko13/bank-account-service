package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.account.dto.ModifyBalanceRequest;
import com.chriniko.revolut.hometask.error.ErrorDetails;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AccountDebitRouteIT extends SpecificationIT implements AccountGenerator {

    @Before
    public void setup() {
        accountRepository.clear();
    }

    @Test
    public void debit_account_normal_case() throws Exception {

        // given
        //create_section
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());
        createAccountResponse.close();
        long accountId = 1;

        //credit_section
        BigDecimal creditAmount = BigDecimal.valueOf(10000D);
        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();

        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();

        assertEquals(200, creditAccountResponse.code());

        creditAccountResponse.close();


        //find_section
        Request getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        Response getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justCreatedAccountAsString = getAccountResponse.body().string();
        String expectedCreatedAccount = FileSupport.read("test/response/debit/account_debit_account_creation_normal_case.json");

        BigDecimal justCreatedAccountBalanceAmount = objectMapper.readValue(justCreatedAccountAsString, AccountDto.class)
                .getBalance()
                .getAmount();

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );

        getAccountResponse.close();

        // when
        BigDecimal debitAmount = BigDecimal.valueOf(2000.32D);

        modifyBalanceRequest = new ModifyBalanceRequest(debitAmount);

        RequestBody debitAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request debitAccountRequest
                = new Request.Builder()
                .patch(debitAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/debit").build();


        Response debitAccountResponse = httpClient.newCall(debitAccountRequest).execute();


        // then
        assertEquals(200, debitAccountResponse.code());


        getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String accountUpdatedAfterDebitOperation = getAccountResponse.body().string();
        String expectedAccountInfoAfterCreditOperation = FileSupport.read("test/response/debit/account_debit_normal_case.json");

        JSONAssert.assertEquals(expectedAccountInfoAfterCreditOperation, accountUpdatedAfterDebitOperation,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );

        BigDecimal newBalanceAmount = justCreatedAccountBalanceAmount.subtract(debitAmount);
        BigDecimal accountAmount = objectMapper.readValue(accountUpdatedAfterDebitOperation, AccountDto.class).getBalance().getAmount();
        assertEquals(newBalanceAmount, accountAmount);


        // clean up
        debitAccountResponse.close();
        getAccountResponse.close();

    }

    @Test
    public void debit_account_validation_case() throws Exception {

        // given
        //create_section
        ModifyAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        RequestBody requestBody = RequestBody.create(null, payload);
        Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

        Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

        assertEquals(201, createAccountResponse.code());
        createAccountResponse.close();
        long accountId = 1;

        //credit_section
        BigDecimal creditAmount = BigDecimal.valueOf(10000D);
        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(creditAmount);

        RequestBody creditAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request creditAccountRequest
                = new Request.Builder()
                .patch(creditAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/credit").build();

        Response creditAccountResponse = httpClient.newCall(creditAccountRequest).execute();

        assertEquals(200, creditAccountResponse.code());

        creditAccountResponse.close();


        //find_section
        Request getAccountRequest = new Request.Builder().get().url(baseUrl + "/accounts/" + accountId).build();
        Response getAccountResponse = httpClient.newCall(getAccountRequest).execute();

        String justCreatedAccountAsString = getAccountResponse.body().string();
        String expectedCreatedAccount = FileSupport.read("test/response/debit/account_debit_account_creation_normal_case.json");

        BigDecimal justCreatedAccountBalanceAmount = objectMapper.readValue(justCreatedAccountAsString, AccountDto.class)
                .getBalance()
                .getAmount();

        JSONAssert.assertEquals(expectedCreatedAccount, justCreatedAccountAsString,
                new CustomComparator(JSONCompareMode.STRICT,
                        new Customization("created", (o1, o2) -> true),
                        new Customization("updated", (o1, o2) -> true)
                )
        );

        getAccountResponse.close();

        // when
        BigDecimal debitAmount = BigDecimal.valueOf(-2000.32D);

        modifyBalanceRequest = new ModifyBalanceRequest(debitAmount);

        RequestBody debitAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request debitAccountRequest
                = new Request.Builder()
                .patch(debitAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/debit").build();


        Response debitAccountResponse = httpClient.newCall(debitAccountRequest).execute();


        // then
        assertEquals(400, debitAccountResponse.code());

        String actual = debitAccountResponse.body().string();

        ErrorDetails errorDetails = objectMapper.readValue(actual, ErrorDetails.class);
        List<String> messages = errorDetails.getMessages();

        assertEquals(
                new HashSet<>(Collections.singletonList("amount -> must be greater than 0")),
                new HashSet<>(messages)
        );

        debitAccountResponse.close();

    }

    @Test
    public void debit_account_insufficient_funds_case() throws Exception {


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
        BigDecimal debitAmount = BigDecimal.valueOf(11000.49);

        ModifyBalanceRequest modifyBalanceRequest = new ModifyBalanceRequest(debitAmount);

        RequestBody debitAccountRequestBody = RequestBody.create(null, objectMapper.writeValueAsString(modifyBalanceRequest));

        Request debitAccountRequest
                = new Request.Builder()
                .patch(debitAccountRequestBody)
                .url(baseUrl + "/accounts/" + accountId + "/debit").build();


        Response debitAccountResponse = httpClient.newCall(debitAccountRequest).execute();


        // then
        assertEquals(400, debitAccountResponse.code());

        String body = debitAccountResponse.body().string();
        String expected = FileSupport.read("test/response/debit/account_debit_insufficient_funds_case.json");

        JSONAssert.assertEquals(expected, body,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("timestamp", (o1, o2) -> true)
                )
        );


        // clean up
        debitAccountResponse.close();

    }

}
