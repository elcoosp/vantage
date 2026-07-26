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
        DatabaseContextHolder.setDatabaseType(type);
        try {
            return pjp.proceed();
        } finally {
            // Keep captured for test verification
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
