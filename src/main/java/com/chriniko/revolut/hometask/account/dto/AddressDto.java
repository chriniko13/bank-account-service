package com.chriniko.revolut.hometask.account.dto;

import com.chriniko.revolut.hometask.account.entity.Address;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class AddressDto {

    @NotEmpty
    private String name;

    @NotEmpty
    private String streetAddress;

    @NotEmpty
    private String city;

    @NotEmpty
    private String state;

    @NotEmpty
    private String zipCode;

    public AddressDto(Address address) {
        this.name = address.getName();
        this.streetAddress = address.getStreetAddress();
        this.city = address.getCity();
        this.state = address.getState();
        this.zipCode = address.getZipCode();
    }
}
