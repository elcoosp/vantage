package com.vantage.core.config;

import com.vantage.core.ratelimiter.RateLimitInterceptor;
import com.vantage.core.tenant.TenantFilterInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/v1/vendors/register", "/actuator/**");
    }

    @Bean
    public HibernatePropertiesCustomizer tenantFilterInterceptorCustomizer(TenantFilterInterceptor tenantFilterInterceptor) {
        return hibernateProperties -> hibernateProperties.put(AvailableSettings.INTERCEPTOR, tenantFilterInterceptor);
    }
}
