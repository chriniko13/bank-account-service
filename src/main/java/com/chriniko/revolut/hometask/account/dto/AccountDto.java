package com.chriniko.revolut.hometask.account.dto;

import com.chriniko.revolut.hometask.account.entity.Account;
import com.chriniko.revolut.hometask.serde.InstantDeserializer;
import com.chriniko.revolut.hometask.serde.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {

    private Long id;

    private NameDto name;
    private AddressDto address;
    private BalanceDto balance;

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant created;

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant updated;

    public AccountDto(Account account) {
        this.id = account.getId();
        this.name = new NameDto(account.getName());
        this.address = new AddressDto(account.getAddress());
        this.balance = new BalanceDto(account.getBalance());
        this.created = account.getCreated();
        this.updated = account.getUpdated();
    }
}
