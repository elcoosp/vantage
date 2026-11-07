package com.vantage.integration.security;

import com.vantage.core.security.TenantFilterActivator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    TenantSecurityFilter tenantSecurityFilter,
                                                    TenantFilterActivator tenantFilterActivator) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/vendors/register").permitAll()
                .requestMatchers("/api/v1/vendors/login").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // The WebSocket handshake is public at the HTTP layer;
                // authentication is enforced per STOMP CONNECT frame by
                // WsJwtAuthChannelInterceptor (the JWT travels as a STOMP
                // header, not an HTTP header, and cannot pass this filter).
                .requestMatchers("/ws/**", "/ws").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(tenantSecurityFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(tenantFilterActivator, TenantSecurityFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
