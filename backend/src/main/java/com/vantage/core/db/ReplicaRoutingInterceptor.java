package com.vantage.core.db;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

public class ReplicaRoutingInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            // Check class-level annotation
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        if (transactional != null && transactional.readOnly()) {
            DatabaseContextHolder.setDatabaseType(DatabaseType.REPLICA);
        } else {
            DatabaseContextHolder.setDatabaseType(DatabaseType.PRIMARY);
        }

        try {
            return invocation.proceed();
        } finally {
            DatabaseContextHolder.clear();
        }
    }
}
