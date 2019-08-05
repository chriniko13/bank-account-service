package com.chriniko.revolut.hometask.account.entity;

import lombok.Data;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

@Data
public class Account {

    private Long id;

    private Name name;
    private Address address;
    private Balance balance;

    private List<Transaction> transactions = new LinkedList<>();

    private Instant created;
    private Instant updated;

    private long optimisticReadStamp;

}
