package com.chriniko.revolut.hometask.account.dto;

import com.chriniko.revolut.hometask.account.entity.Address;
import com.chriniko.revolut.hometask.account.entity.Balance;
import com.chriniko.revolut.hometask.account.entity.Name;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateAccountRequest {

    private Name name;
    private Address address;
    private Balance balance;

}
