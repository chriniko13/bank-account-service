package com.chriniko.revolut.hometask.it.core;

import org.jboss.weld.proxy.WeldClientProxy;

public interface CdiHelper {


    @SuppressWarnings("unchecked")
    default <T> T getNonProxiedObject(T proxiedObject) {
        if (!(proxiedObject instanceof WeldClientProxy)) {
            throw new TestInfraException("not cdi proxied object");
        }
        return (T) ((WeldClientProxy) proxiedObject).getMetadata().getContextualInstance();
    }
}
