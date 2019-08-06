package com.chriniko.revolut.hometask.error;

import com.chriniko.revolut.hometask.entity.IdentifiableLong;
import io.undertow.util.Headers;
import lombok.Getter;

@Getter
public class EntityModifiedSinceLastViewException extends RuntimeException {

    private final long id;
    private final Class<? extends IdentifiableLong> clazz;

    public EntityModifiedSinceLastViewException(long id, Class<? extends IdentifiableLong> clazz) {
        this.id = id;
        this.clazz = clazz;
    }

    @Override
    public String getMessage() {
        return clazz.getSimpleName() + " with id: " + id
                + " modified since your last view, "
                + "fetch it again to see the changes or not provide value for header->" + Headers.ETAG
                + ", in order to force write lock";
    }
}
