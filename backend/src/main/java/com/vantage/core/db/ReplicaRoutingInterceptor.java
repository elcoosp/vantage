package com.vantage.core.db;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

@Aspect
@Component
public class ReplicaRoutingInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ReplicaRoutingInterceptor.class);
    private static final ThreadLocal<DatabaseType> LAST_DECISION = new ThreadLocal<>();

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object route(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
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
        // Capture for test verification
        LAST_DECISION.set(type);
        log.info("ReplicaRoutingInterceptor setting context to: {}", type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return pjp.proceed();
        } finally {
            log.info("ReplicaRoutingInterceptor clearing context");
            DatabaseContextHolder.clear();
        }
    }

    public static DatabaseType getLastDecision() {
        return LAST_DECISION.get();
    }

    public static void clearDecision() {
        LAST_DECISION.remove();
    }
}
