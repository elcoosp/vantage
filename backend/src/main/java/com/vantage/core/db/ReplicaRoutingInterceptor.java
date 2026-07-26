package com.vantage.core.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

public class ReplicaRoutingInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ReplicaRoutingInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        DatabaseType type;
        if (transactional != null && transactional.readOnly()) {
            type = DatabaseType.REPLICA;
        } else {
            type = DatabaseType.PRIMARY;
        }
        log.info("ReplicaRoutingInterceptor setting context to: {}", type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return invocation.proceed();
        } finally {
            log.info("ReplicaRoutingInterceptor clearing context");
            DatabaseContextHolder.clear();
        }
    }
}