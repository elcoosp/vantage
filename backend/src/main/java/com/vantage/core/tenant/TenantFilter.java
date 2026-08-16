package com.vantage.core.tenant;

import com.vantage.core.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Resolves the active tenant for every request and publishes it on {@link TenantContext}.
 *
 * <p>The tenant is taken from the {@code X-Tenant-ID} header when present, and otherwise from a
 * valid {@code Authorization: Bearer} JWT. Establishing the tenant from either identifier (rather
 * than demanding the header) keeps the context consistent for clients that authenticate with a
 * token only, and lets integration tests authenticate with a Bearer token without also threading
 * the header through every call.</p>
 *
 * <p>Registered as a servlet filter at the highest precedence (see {@code TenantFilterConfig}) so the
 * tenant context is available to downstream components that execute inside the security filter
 * chain and the Spring MVC interceptor stack (e.g. the rate-limit interceptor and GraphQL
 * resolvers), which would otherwise observe a null context.</p>
 */
public class TenantFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public TenantFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        UUID tenantId = resolveTenantId(request);

        if (tenantId != null) {
            TenantContext.setTenantId(tenantId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private UUID resolveTenantId(HttpServletRequest request) {
        String headerTenant = request.getHeader("X-Tenant-ID");
        if (headerTenant != null && !headerTenant.isBlank()) {
            try {
                return UUID.fromString(headerTenant);
            } catch (IllegalArgumentException ignored) {
                // fall through to JWT resolution
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            try {
                return jwtService.extractTenantId(authHeader.substring(BEARER_PREFIX.length()));
            } catch (RuntimeException ignored) {
                // invalid token -> no tenant
            }
        }

        return null;
    }
}
