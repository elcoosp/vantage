package com.vantage.core.websocket.config;

import com.vantage.core.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Validates the JWT presented in the STOMP CONNECT frame and binds the
 * session to a tenant {@link Principal} named {@code tenant:<uuid>}.
 *
 * <p>Without this the raw broker would be an unauthenticated, cross-tenant
 * data leak: any browser could subscribe to any tenant's ops-map topic.</p>
 */
public class WsJwtAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WsJwtAuthChannelInterceptor.class);

    private final JwtService jwtService;

    public WsJwtAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String tenantId = authenticate(accessor);
            accessor.setUser(new TenantPrincipal(tenantId));
            log.debug("STOMP CONNECT authenticated for tenant {}", tenantId);
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (user == null) {
                throw new IllegalArgumentException("SUBSCRIBE requires an authenticated session");
            }
            String destination = accessor.getDestination();
            String tenantId = ((TenantPrincipal) user).tenantId().toString();
            // /topic/ops-map.<tenantId> is the only per-tenant broadcast channel.
            if (destination == null || !destination.startsWith("/topic/ops-map." + tenantId)) {
                throw new IllegalArgumentException("Subscription to " + destination + " is not allowed for this tenant");
            }
        }
        return message;
    }

    private String authenticate(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            throw new IllegalArgumentException("Missing Authorization header in STOMP CONNECT");
        }
        String value = authorization.get(0);
        if (!value.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization header must use the Bearer scheme");
        }
        try {
            UUID tenantId = jwtService.extractTenantId(value.substring("Bearer ".length()));
            return tenantId.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JWT in STOMP CONNECT", e);
        }
    }

    public record TenantPrincipal(String tenantId) implements Principal {
        @Override
        public String getName() {
            return "tenant:" + tenantId;
        }
    }
}
