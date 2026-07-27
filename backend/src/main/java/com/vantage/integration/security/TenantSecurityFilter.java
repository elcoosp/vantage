package com.vantage.integration.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantage.core.tenant.TenantContext;
import com.vantage.integration.domain.ApiKey;
import com.vantage.integration.domain.ApiKeyRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TenantSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantSecurityFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtService jwtService;
    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantSecurityFilter(JwtService jwtService, ApiKeyRepository apiKeyRepository, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.apiKeyRepository = apiKeyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        boolean authenticated = false;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                UUID tenantId = jwtService.extractTenantId(token);
                TenantContext.setTenantId(tenantId);
                authenticated = true;
                log.info("Authenticated via JWT for tenant: {}", tenantId);
                // Set SecurityContext
                Authentication authentication = new UsernamePasswordAuthenticationToken(tenantId.toString(), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("Invalid JWT token provided: {}", e.getMessage());
            }
        }

        if (!authenticated) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey != null && !apiKey.isBlank()) {
                log.info("Checking API key: {}", apiKey);
                authenticated = authenticateWithApiKey(apiKey, response);
                if (authenticated) {
                    log.info("API key authentication succeeded");
                    // Set SecurityContext
                    UUID tenantId = TenantContext.getTenantId();
                    if (tenantId != null) {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(tenantId.toString(), null, List.of());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else {
                    log.info("API key authentication failed");
                }
            }
        }

        if (!authenticated && !isPublicEndpoint(request)) {
            log.info("Request is not authenticated and not public, returning 401");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            OBJECT_MAPPER.writeValue(response.getWriter(), Map.of("error", "Unauthorized", "message", "Authentication required"));
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear security context and tenant context after request
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    private boolean authenticateWithApiKey(String apiKey, HttpServletResponse response) {
        if (apiKey.length() < 12) {
            log.warn("API key too short: length {}", apiKey.length());
            return false;
        }
        String prefix = apiKey.substring(0, 12);
        log.info("Searching for API key with prefix: {}", prefix);
        List<ApiKey> keys = apiKeyRepository.findByKeyPrefixAndRevokedFalse(prefix);
        log.info("Found {} keys with that prefix", keys.size());
        for (ApiKey key : keys) {
            log.info("Checking key with id: {}, hash: {}", key.getId(), key.getKeyHash());
            if (passwordEncoder.matches(apiKey, key.getKeyHash())) {
                log.info("API key matched for tenant: {}", key.getTenantId());
                TenantContext.setTenantId(key.getTenantId());
                key.setLastUsedAt(Instant.now());
                apiKeyRepository.save(key);
                return true;
            }
        }
        log.info("No matching API key found");
        return false;
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/vendors/register") || path.startsWith("/actuator/health");
    }
}
