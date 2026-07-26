package com.vantage.core.db;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

@Aspect
@Component
public class TestRoutingAspect {
    private static final ThreadLocal<DatabaseType> CAPTURED = new ThreadLocal<>();

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object captureRouting(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }
        DatabaseType type = (transactional != null && transactional.readOnly()) ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        CAPTURED.set(type);
        // Also set the DatabaseContextHolder so the DataSource uses the correct one
        DatabaseContextHolder.setDatabaseType(type);
        try {
            return pjp.proceed();
        } finally {
            // Do not clear here; we'll clear in test teardown after capturing
            // But we need to clear the DatabaseContextHolder after the request?
            // The aspect is around the service method, which is inside the request.
            // After the method returns, the filter chain continues. We want to keep the context
            // for the test to assert. We'll clear it in the test's @AfterEach.
        }
    }

    public static DatabaseType getCaptured() {
        return CAPTURED.get();
    }

    public static void clearCaptured() {
        CAPTURED.remove();
        DatabaseContextHolder.clear();
    }
}
