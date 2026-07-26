package com.vantage.core.db;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class TestRoutingInterceptor implements MethodInterceptor {
    public static final AtomicReference<DatabaseType> captured = new AtomicReference<>();

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
        // Store the decision before the context is set
        captured.set(type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return invocation.proceed();
        } finally {
            DatabaseContextHolder.clear();
            // Keep captured for test verification
        }
    }

    public static void clear() {
        captured.set(null);
    }
}
