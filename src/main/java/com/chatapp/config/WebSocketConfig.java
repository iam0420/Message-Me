package com.chatapp.config;

import com.chatapp.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.*;
import org.springframework.web.socket.config.annotation.*;

@Configuration @EnableWebSocketMessageBroker @RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor wsAuth;

    @Override public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic","/queue","/user");
        r.setApplicationDestinationPrefixes("/app");
        r.setUserDestinationPrefix("/user");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
        r.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
    @Override public void configureClientInboundChannel(ChannelRegistration r) {
        r.interceptors(wsAuth);
    }
    @Override public void configureWebSocketTransport(WebSocketTransportRegistration r) {
        r.setMessageSizeLimit(128*1024).setSendBufferSizeLimit(512*1024).setSendTimeLimit(20000);
    }
}
