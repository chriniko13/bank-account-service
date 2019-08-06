package com.chriniko.revolut.hometask.account.entity;

import com.chriniko.revolut.hometask.account.dto.AddressDto;
import com.chriniko.revolut.hometask.account.dto.NameDto;
import com.chriniko.revolut.hometask.entity.IdentifiableLong;
import com.chriniko.revolut.hometask.serde.InstantDeserializer;
import com.chriniko.revolut.hometask.serde.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Data
public class Account extends IdentifiableLong {

    private Long id;

    private Name name;
    private Address address;

    // Note: balance should only change from operations such as: debit, credit, transfer. NOT by update account info operations.
    private Balance balance = new Balance();

    private final List<Transaction> transactions = new LinkedList<>();

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant created;

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant updated;

    private long optimisticReadStamp;

    public Account(NameDto nameDto, AddressDto addressDto) {
        this.name = new Name(nameDto);
        this.address = new Address(addressDto);
    }

    public Account(long accountId, NameDto nameDto, AddressDto addressDto) {
        this(nameDto, addressDto);
        this.id = accountId;
    }

    public void applyTransaction(Transaction tx) {

        switch (tx.getTransactionType()) {
            case CREDIT:
                balance.setAmount(
                        balance.getAmount().add(tx.getAmount())
                );
                break;

            case DEBIT:
                balance.setAmount(
                        balance.getAmount().subtract(tx.getAmount())
                );
                break;

            default:
                throw new IllegalStateException("not valid transaction type");
        }

        transactions.add(tx);
    }
}
