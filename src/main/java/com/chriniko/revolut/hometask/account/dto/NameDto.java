package com.chriniko.revolut.hometask.account.dto;

import com.chriniko.revolut.hometask.account.entity.Name;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NameDto {

    private String first;
    private String initials;
    private String last;

    public NameDto(Name name) {
        this.first = name.getFirst();
        this.initials = name.getInitials();
        this.last = name.getLast();
    }
}
