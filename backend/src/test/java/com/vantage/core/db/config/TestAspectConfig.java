package com.vantage.core.db.config;

import com.vantage.core.db.TestRoutingInterceptor;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

@Configuration
public class TestAspectConfig {

    @Bean
    @Primary
    public TestRoutingInterceptor testRoutingInterceptor() {
        return new TestRoutingInterceptor();
    }

    @Bean
    @Primary
    public Advisor testRoutingAdvisor(TestRoutingInterceptor interceptor) {
        AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Transactional.class);
        return new DefaultPointcutAdvisor(pointcut, interceptor);
    }
}
