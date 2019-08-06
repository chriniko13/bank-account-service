package com.chriniko.revolut.hometask.validation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@ApplicationScoped
public class ValidatorProducer {


    @Produces
    @ApplicationScoped
    public ValidatorFactory validatorFactory() {
        return Validation.buildDefaultValidatorFactory();
    }


}
