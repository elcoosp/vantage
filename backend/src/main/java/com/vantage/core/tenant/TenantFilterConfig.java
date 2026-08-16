package com.vantage.core.tenant;

import com.vantage.core.security.JwtService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link TenantFilter} as a servlet filter at the highest precedence so the tenant context
 * is established before the Spring Security filter chain and the Spring MVC interceptor stack run.
 *
 * <p>Without explicit ordering a {@code @Component} filter is auto-registered after the security
 * {@code FilterChainProxy}, which leaves the tenant context null for components that execute inside
 * the chain (the rate-limit interceptor, GraphQL resolvers). Registering via
 * {@link FilterRegistrationBean} with {@link Ordered#HIGHEST_PRECEDENCE} guarantees the tenant is
 * resolved first, for both the production and the test security chains.</p>
 */
@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter realTenantFilter(JwtService jwtService) {
        return new TenantFilter(jwtService);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter tenantFilter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(tenantFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
