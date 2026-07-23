package com.vantage.core.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String tenantIdHeader = request.getHeader("X-Tenant-ID");

        if (tenantIdHeader == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-ID header");
            return;
        }

        try {
            UUID tenantId = UUID.fromString(tenantIdHeader);
            TenantContext.setTenantId(tenantId);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-ID format");
        } finally {
            TenantContext.clear();
        }
    }
}
