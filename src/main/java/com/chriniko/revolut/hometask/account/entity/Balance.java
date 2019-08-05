package com.chriniko.revolut.hometask.account.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Balance {

    private BigDecimal amount;
    private Currency currency = Currency.EURO;

}
