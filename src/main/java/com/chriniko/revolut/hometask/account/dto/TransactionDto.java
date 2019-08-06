package com.chriniko.revolut.hometask.account.dto;


import com.chriniko.revolut.hometask.account.entity.Transaction;
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
public class TransactionDto {


    private String transactionType;

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant timestamp;

    private BigDecimal amount;

    private String currency;

    private String details;

    public TransactionDto(Transaction transaction) {
        this.transactionType = transaction.getTransactionType().name();
        this.timestamp = transaction.getTimestamp();
        this.amount = transaction.getAmount();
        this.currency = transaction.getCurrency().name();
        this.details = transaction.getDetails();
    }
}
