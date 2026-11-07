package com.vantage.core.websocket.config;

import com.vantage.core.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time STOMP endpoint backing the Live Ops Map.
 *
 * <p>Endpoint: {@code GET /ws} (raw WebSocket — no SockJS, matching the
 * {@code @stomp/stompjs} client). The HTTP handshake is permitted by
 * Spring Security; authentication happens per STOMP CONNECT frame via
 * {@link WsJwtAuthChannelInterceptor}, because the browser client sends its
 * JWT as a STOMP header, not as an HTTP header.</p>
 *
 * <p>Broadcast channels: {@code /topic/ops-map.<tenantId>} — one channel per
 * tenant, populated by {@code OpsMapPublisher} from order lifecycle events.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client destinations are not used yet, but /app is reserved for
        // future request/reply interactions (e.g. subscribing with filters).
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WsJwtAuthChannelInterceptor(jwtService));
    }
}
