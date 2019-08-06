package com.chriniko.revolut.hometask.account.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;

@Data
@NoArgsConstructor
public class ModifyAccountRequest {

    @Valid
    private NameDto name;

    @Valid
    private AddressDto address;

}
