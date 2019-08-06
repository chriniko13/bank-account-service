package com.chriniko.revolut.hometask.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModifyBalanceRequest {

    @Positive
    @NotNull
    private BigDecimal amount;

    private String details;

    public ModifyBalanceRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public ModifyBalanceRequest(double amount) {
        this.amount = BigDecimal.valueOf(amount);
    }
}
