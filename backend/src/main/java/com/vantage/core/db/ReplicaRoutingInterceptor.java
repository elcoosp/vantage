package com.vantage.core.db;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

public class ReplicaRoutingInterceptor implements MethodInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ReplicaRoutingInterceptor.class);
    private static final ThreadLocal<DatabaseType> lastDecision = new ThreadLocal<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        DatabaseType type = (transactional != null && transactional.readOnly()) ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        lastDecision.set(type);
        log.info("ReplicaRoutingInterceptor setting context to: {}", type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return invocation.proceed();
        } finally {
            log.info("ReplicaRoutingInterceptor clearing context");
            DatabaseContextHolder.clear();
        }
    }

    public static DatabaseType getLastDecision() {
        return lastDecision.get();
    }

    public static void clearDecision() {
        lastDecision.remove();
    }
}
