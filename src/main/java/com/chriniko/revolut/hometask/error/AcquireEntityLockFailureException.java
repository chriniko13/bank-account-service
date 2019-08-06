package com.chriniko.revolut.hometask.error;

import com.chriniko.revolut.hometask.entity.IdentifiableLong;
import lombok.Getter;

@Getter
public class AcquireEntityLockFailureException extends RuntimeException {

    private final long id;
    private final Class<? extends IdentifiableLong> clazz;

    public AcquireEntityLockFailureException(long id, Class<? extends IdentifiableLong> clazz) {
        this.id = id;
        this.clazz = clazz;
    }

    @Override
    public String getMessage() {
        return clazz.getSimpleName() + " with id: " + id + " is under modification, try again later";
    }
}
