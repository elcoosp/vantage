package com.vantage.core.db;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

@Aspect
public class TestReplicaRoutingInterceptor {
    private static final AtomicReference<DatabaseType> lastDecision = new AtomicReference<>();

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object route(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        DatabaseType type = (transactional != null && transactional.readOnly()) ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        lastDecision.set(type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return pjp.proceed();
        } finally {
            DatabaseContextHolder.clear();
        }
    }

    public static DatabaseType getLastDecision() {
        return lastDecision.get();
    }

    public static void clearDecision() {
        lastDecision.set(null);
    }
}
