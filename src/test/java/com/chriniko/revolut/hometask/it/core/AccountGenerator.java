package com.chriniko.revolut.hometask.it.core;

import com.chriniko.revolut.hometask.account.dto.AddressDto;
import com.chriniko.revolut.hometask.account.dto.ModifyAccountRequest;
import com.chriniko.revolut.hometask.account.dto.NameDto;
import com.github.javafaker.Faker;

public interface AccountGenerator {

    default ModifyAccountRequest createSampleAccountRequest() {

        ModifyAccountRequest createAccount = new ModifyAccountRequest();

        NameDto name = new NameDto();
        name.setFirst("John");
        name.setLast("Doe");
        createAccount.setName(name);

        AddressDto address = new AddressDto();
        address.setCity("city");
        address.setName("name");
        address.setState("state");
        address.setStreetAddress("street");
        address.setZipCode("1234");
        createAccount.setAddress(address);


        return createAccount;
    }

    default ModifyAccountRequest createSampleAccountRequest(Faker faker) {

        ModifyAccountRequest createAccount = new ModifyAccountRequest();

        NameDto name = new NameDto();
        name.setFirst(faker.name().firstName());
        name.setLast(faker.name().lastName());
        createAccount.setName(name);

        AddressDto address = new AddressDto();
        address.setCity(faker.address().city());
        address.setName(faker.address().cityName());
        address.setState(faker.address().state());
        address.setStreetAddress(faker.address().streetAddress());
        address.setZipCode(faker.address().zipCode());
        createAccount.setAddress(address);

        return createAccount;
    }

}
