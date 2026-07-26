package com.vantage.core.db;

import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        log.info("ReplicaRoutingInterceptor bean initialized");
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object route(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        if (transactional == null) {
            transactional = method.getDeclaringClass().getAnnotation(Transactional.class);
        }

        DatabaseType type = (transactional != null && transactional.readOnly()) ? DatabaseType.REPLICA : DatabaseType.PRIMARY;
        log.info("ReplicaRoutingInterceptor setting context to: {}", type);
        DatabaseContextHolder.setDatabaseType(type);

        try {
            return pjp.proceed();
        } finally {
            log.info("ReplicaRoutingInterceptor clearing context");
            DatabaseContextHolder.clear();
        }
    }
}
