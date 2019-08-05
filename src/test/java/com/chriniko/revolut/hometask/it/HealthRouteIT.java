package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class HealthRouteIT extends SpecificationIT {

    @Test
    public void health_route_normal_case() throws Exception {

        // given
        Request request = new Request.Builder().url(baseUrl + "/health").build();

        // when
        try (Response response = httpClient.newCall(request).execute();) {

            // then
            String payloadAsString = response.body().string();

            String expected = FileSupport.read("test/response/health_route_normal_response.json");

            JSONAssert.assertEquals(payloadAsString, expected, true);
        }
    }

}
