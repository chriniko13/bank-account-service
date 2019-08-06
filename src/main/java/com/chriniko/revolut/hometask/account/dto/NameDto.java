package com.chriniko.revolut.hometask.account.dto;

import com.chriniko.revolut.hometask.account.entity.Name;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class NameDto {

    @NotEmpty
    private String first;

    private String initials;

    @NotEmpty
    private String last;

    public NameDto(Name name) {
        this.first = name.getFirst();
        this.initials = name.getInitials();
        this.last = name.getLast();
    }
}
