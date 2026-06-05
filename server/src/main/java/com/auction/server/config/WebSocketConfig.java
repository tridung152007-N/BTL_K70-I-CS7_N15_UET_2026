package com.auction.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Bật cả /topic (broadcast) và /queue (gửi riêng từng user)
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        // Prefix để convertAndSendToUser biết route đúng
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket endpoint (không SockJS)
        registry.addEndpoint("/ws-auction")
                .setAllowedOriginPatterns("*");

        // SockJS endpoint (fallback cho browser cũ)
        registry.addEndpoint("/ws-auction-sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Legacy endpoint (keep for backward compatibility)
        registry.addEndpoint("/ws-auction-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(256 * 1024);
        registry.setSendBufferSizeLimit(256 * 1024);
        registry.setSendTimeLimit(20000);
        System.out.println("✅ SERVER WebSocket transport configured: messageSize=256KB, sendBuffer=256KB");
    }
}