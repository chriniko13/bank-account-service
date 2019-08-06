package com.chriniko.revolut.hometask.account.entity;

import com.chriniko.revolut.hometask.serde.InstantDeserializer;
import com.chriniko.revolut.hometask.serde.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    private TransactionType transactionType;

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant timestamp;

    private BigDecimal amount;

    private Currency currency;

    private String details;

}
