package com.vantage.core.db;

import jakarta.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

@Aspect
public class ReplicaRoutingInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ReplicaRoutingInterceptor.class);
    private static final ThreadLocal<DatabaseType> lastDecision = new ThreadLocal<>();

    static {
        log.info("ReplicaRoutingInterceptor class loaded");
    }

    @PostConstruct
    public void init() {
        log.info("ReplicaRoutingInterceptor bean initialized (PostConstruct)");
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object route(ProceedingJoinPoint pjp) throws Throwable {
        log.info("ReplicaRoutingInterceptor.route() invoked for method: {}", pjp.getSignature());
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        DatabaseType type = (transactional != null && transactional.readOnly()) ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        lastDecision.set(type);
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
        return lastDecision.get();
    }

    public static void clearDecision() {
        lastDecision.remove();
    }
}
