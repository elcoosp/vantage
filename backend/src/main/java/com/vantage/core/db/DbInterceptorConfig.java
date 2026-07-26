package com.vantage.core.db;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class DbInterceptorConfig {

    @Bean
    public ReplicaRoutingInterceptor replicaRoutingInterceptor() {
        return new ReplicaRoutingInterceptor();
    }

    @Bean
    public Advisor replicaRoutingAdvisor(ReplicaRoutingInterceptor interceptor) {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Transactional.class);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }
}
