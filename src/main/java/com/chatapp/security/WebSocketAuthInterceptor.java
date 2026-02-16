package com.chatapp.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.util.List;

@Component @Slf4j @RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeader = accessor.getNativeHeader("Authorization");
            if (authHeader != null && !authHeader.isEmpty()) {
                String token = authHeader.get(0);
                if (token.startsWith("Bearer ")) token = token.substring(7);
                if (tokenProvider.validateToken(token)) {
                    Long userId = tokenProvider.getUserIdFromJWT(token);
                    UserDetails userDetails = userDetailsService.loadUserById(userId);
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(auth);
                    log.info("WS authenticated: {}", userDetails.getUsername());
                }
            }
        }
        return message;
    }
}
