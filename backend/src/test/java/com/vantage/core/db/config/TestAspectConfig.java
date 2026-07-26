package com.vantage.core.db.config;

import com.vantage.core.db.ReplicaRoutingInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableAspectJAutoProxy
public class TestAspectConfig {

    @Bean
    public Advisor replicaRoutingAdvisor() {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Transactional.class);
        return new DefaultPointcutAdvisor(pointcut, new ReplicaRoutingInterceptor());
    }
}
