package com.chriniko.revolut.hometask.it;

import com.chriniko.revolut.hometask.it.core.FileSupport;
import com.chriniko.revolut.hometask.it.core.SpecificationIT;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ErrorHandlingIT extends SpecificationIT {

    @Test
    public void processing_exception_case() throws Exception {

        // given
        Request request = new Request.Builder().url(baseUrl + "/error-handling-demonstration?error-type=processing").build();

        // when
        try (Response response = httpClient.newCall(request).execute();) {

            // then
            String payloadAsString = response.body().string();

            String expected = FileSupport.read("test/response/error_handling_processing_case.json");

            JSONAssert.assertEquals(payloadAsString, expected, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));
        }

    }

    @Test
    public void validation_exception_case() throws Exception {

        // given
        Request request = new Request.Builder().url(baseUrl + "/error-handling-demonstration?error-type=validation").build();

        // when
        try (Response response = httpClient.newCall(request).execute();) {

            // then
            String payloadAsString = response.body().string();

            String expected = FileSupport.read("test/response/error_handling_validation_case.json");

            JSONAssert.assertEquals(payloadAsString, expected, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));
        }

    }

    @Test
    public void unknown_exception_case() throws Exception {

        // given
        Request request = new Request.Builder().url(baseUrl + "/error-handling-demonstration?error-type=other").build();

        // when
        try (Response response = httpClient.newCall(request).execute();) {

            // then
            String payloadAsString = response.body().string();

            String expected = FileSupport.read("test/response/error_handling_unknown_case.json");

            JSONAssert.assertEquals(payloadAsString, expected, new CustomComparator(JSONCompareMode.STRICT, new Customization("timestamp", (o1, o2) -> true)));
        }

    }
}
