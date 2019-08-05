package com.chriniko.revolut.hometask.account.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Transaction {

    private TransactionType transactionType;
    private Instant timestamp;
    private BigDecimal amount;
    private String details;

}
