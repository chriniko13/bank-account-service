package com.chriniko.revolut.hometask.it.core;

import com.chriniko.revolut.hometask.account.dto.CreateAccountRequest;
import com.chriniko.revolut.hometask.account.entity.Address;
import com.chriniko.revolut.hometask.account.entity.Balance;
import com.chriniko.revolut.hometask.account.entity.Name;
import com.github.javafaker.Faker;

import java.math.BigDecimal;

public interface AccountGenerator {

    default CreateAccountRequest createSampleAccountRequest() {

        CreateAccountRequest createAccount = new CreateAccountRequest();

        Name name = new Name();
        name.setFirst("John");
        name.setLast("Doe");
        createAccount.setName(name);

        Address address = new Address();
        address.setCity("city");
        address.setName("name");
        address.setState("state");
        address.setStreetAddress("street");
        address.setZipCode("1234");
        createAccount.setAddress(address);

        Balance balance = new Balance();
        balance.setAmount(BigDecimal.valueOf(11000.48D));

        createAccount.setBalance(balance);

        return createAccount;
    }

    default CreateAccountRequest createSampleAccountRequest(Faker faker) {

        CreateAccountRequest createAccount = new CreateAccountRequest();

        Name name = new Name();
        name.setFirst(faker.name().firstName());
        name.setLast(faker.name().lastName());
        createAccount.setName(name);

        Address address = new Address();
        address.setCity(faker.address().city());
        address.setName(faker.address().cityName());
        address.setState(faker.address().state());
        address.setStreetAddress(faker.address().streetAddress());
        address.setZipCode(faker.address().zipCode());
        createAccount.setAddress(address);

        Balance balance = new Balance();
        balance.setAmount(BigDecimal.valueOf(11000.48D));

        createAccount.setBalance(balance);

        return createAccount;
    }

}
