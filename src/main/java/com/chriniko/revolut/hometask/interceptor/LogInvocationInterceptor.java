package com.chriniko.revolut.hometask.interceptor;

import lombok.extern.log4j.Log4j2;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Log4j2

@Interceptor
@LogInvocation
public class LogInvocationInterceptor {

    @AroundInvoke
    public Object logMethodEntry(InvocationContext ctx) throws Exception {
        //log.trace("Entering method: " + ctx.getMethod().getName());
        return ctx.proceed();
    }

}
