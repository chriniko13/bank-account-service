package com.chriniko.revolut.hometask.account.entity;

import com.chriniko.revolut.hometask.account.dto.NameDto;
import lombok.Data;

@Data
public class Name {

    private String first;
    private String initials;
    private String last;

    public Name(NameDto nameDto) {
        this.first = nameDto.getFirst();
        this.initials = nameDto.getInitials();
        this.last = nameDto.getLast();
    }
}
