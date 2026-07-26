package com.vantage.core.db;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class ReplicaRoutingInterceptor implements MethodInterceptor {
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // No routing logic yet – test will fail
        return invocation.proceed();
    }
}
