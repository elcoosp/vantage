package com.vantage.core.db;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

public class TestRoutingInterceptor implements MethodInterceptor {
    private static final ThreadLocal<DatabaseType> captured = new ThreadLocal<>();

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }
        DatabaseType type = (transactional != null && transactional.readOnly()) ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        captured.set(type);
        // Also set the context for the actual routing
        DatabaseContextHolder.setDatabaseType(type);
        try {
            return invocation.proceed();
        } finally {
            // Do not clear captured; we need it for assertion later.
        }
    }

    public static DatabaseType getCaptured() {
        return captured.get();
    }

    public static void clear() {
        captured.remove();
        DatabaseContextHolder.clear();
    }
}
