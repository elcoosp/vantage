package com.vantage.core.db.config;

import com.vantage.core.db.TestReplicaRoutingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestInterceptorConfig {

    @Bean
    @Primary
    public TestReplicaRoutingInterceptor testReplicaRoutingInterceptor() {
        return new TestReplicaRoutingInterceptor();
    }
}
