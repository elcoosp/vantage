package com.vantage.core.developerlog.ui;

import com.vantage.core.developerlog.app.RequestLog;
import com.vantage.core.developerlog.app.RequestLogBuffer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

/**
 * Captures every API request (method, path, status, duration) into the
 * developer portal's live log stream.
 *
 * <p>Skips actuator endpoints, the WebSocket handshake and the log stream
 * itself (which would otherwise feed back into its own output).</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    private final RequestLogBuffer buffer;

    public RequestLogFilter(RequestLogBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/actuator")
                || uri.startsWith("/ws")
                || uri.startsWith("/api/v1/developer/logs/stream")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                buffer.record(new RequestLog(
                        UUID.randomUUID(),
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        (System.nanoTime() - start) / 1_000_000,
                        Instant.now()));
            } catch (Exception e) {
                log.debug("Failed to record request log: {}", e.getMessage());
            }
        }
    }
}
