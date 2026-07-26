package com.vantage.core.db.config;

import com.vantage.core.db.ReplicaRoutingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestInterceptorConfig {

    @Bean
    public ReplicaRoutingInterceptor replicaRoutingInterceptor() {
        return new ReplicaRoutingInterceptor();
    }
}
