package com.chriniko.revolut.hometask.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class JsonOperationsProducer {

    @Produces
    @Dependent // Note: dependent because some client want to enable/disable serde features based on needs.
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
