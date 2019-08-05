package com.chriniko.revolut.hometask.account.dto;

import com.chriniko.revolut.hometask.account.entity.Balance;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BalanceDto {

    private BigDecimal amount;
    private String currency;

    public BalanceDto(Balance balance) {
        this.amount = balance.getAmount();
        this.currency = balance.getCurrency().name();
    }

}
