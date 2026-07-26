package com.vantage.core.db.config;

import com.vantage.core.db.TestRoutingAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestAspectConfig {

    @Bean
    public TestRoutingAspect testRoutingAspect() {
        return new TestRoutingAspect();
    }
}
