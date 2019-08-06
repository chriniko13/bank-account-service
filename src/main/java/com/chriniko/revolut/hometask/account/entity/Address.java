package com.chriniko.revolut.hometask.account.entity;

import com.chriniko.revolut.hometask.account.dto.AddressDto;
import lombok.Data;

@Data
public class Address {

    private String name;
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;

    public Address(AddressDto addressDto) {
        this.name = addressDto.getName();
        this.streetAddress = addressDto.getStreetAddress();
        this.city = addressDto.getCity();
        this.state = addressDto.getState();
        this.zipCode = addressDto.getZipCode();
    }
}
