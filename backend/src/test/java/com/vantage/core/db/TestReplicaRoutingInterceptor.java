package com.vantage.core.db;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

public class TestReplicaRoutingInterceptor implements MethodInterceptor {
    // ThreadLocal to capture the type set during the request for test verification
    private static final ThreadLocal<DatabaseType> CAPTURED_TYPE = new ThreadLocal<>();

    public static DatabaseType getCapturedType() {
        return CAPTURED_TYPE.get();
    }

    public static void clearCapturedType() {
        CAPTURED_TYPE.remove();
    }

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

        // Capture the type for test verification
        CAPTURED_TYPE.set(type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return invocation.proceed();
        } finally {
            // Do not clear the context here; we want to keep it for verification.
            // The test will clear it explicitly.
            // We also keep the captured type.
        }
    }
}
