package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.account.dto.CreateAccountRequest;
import com.chriniko.revolut.hometask.it.core.AccountGenerator;
import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import static org.junit.Assert.assertEquals;

public class AccountFindAllRouteIT extends SpecificationIT implements AccountGenerator {

    @Test
    public void find_all_normal_case() throws Exception {

        // given
        CreateAccountRequest createAccount = createSampleAccountRequest();
        String payload = objectMapper.writeValueAsString(createAccount);

        for (int i = 1; i <= 100; i++) {
            RequestBody requestBody = RequestBody.create(null, payload);
            Request createAccountRequest = new Request.Builder().post(requestBody).url(baseUrl + "/accounts").build();

            Response createAccountResponse = httpClient.newCall(createAccountRequest).execute();

            assertEquals(200, createAccountResponse.code());

            createAccountResponse.close();
        }


        // when
        Request findAllAccountsRequest = new Request.Builder().url(baseUrl + "/accounts").build();
        Response findAllAccountsResponse = httpClient.newCall(findAllAccountsRequest).execute();


        // then
        assertEquals(200, findAllAccountsResponse.code());

        String actual = findAllAccountsResponse.body().string();
        String expected = FileSupport.read("test/response/account_find_all_normal_case.json");

        JSONAssert.assertEquals(expected, actual,
                new CustomComparator(
                        JSONCompareMode.STRICT,
                        new Customization("accounts[*].created", (o1, o2) -> true)
                )
        );

        // clean up
        findAllAccountsResponse.close();

    }

}
