package com.chriniko.revolut.hometask.account.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindAccountTransactionsByIdResponse {

    private List<TransactionDto> transactions;


}
