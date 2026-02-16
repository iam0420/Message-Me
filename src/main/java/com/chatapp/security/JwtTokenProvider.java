package com.chatapp.security;

import com.chatapp.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component @Slf4j @RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtConfig jwtConfig;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(jwtConfig.getSecret().getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication auth) {
        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        return generateTokenFromUserId(up.getId(), up.getUsername());
    }

    public String generateTokenFromUserId(Long userId, String username) {
        Date now = new Date();
        return Jwts.builder()
            .subject(Long.toString(userId))
            .claim("username", username)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + jwtConfig.getExpiration()))
            .signWith(getSigningKey())
            .compact();
    }

    public Long getUserIdFromJWT(String token) {
        return Long.parseLong(Jwts.parser().verifyWith(getSigningKey()).build()
            .parseSignedClaims(token).getPayload().getSubject());
    }

    public String getUsernameFromJWT(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
            .parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("JWT validation error: {}", ex.getMessage());
            return false;
        }
    }
}
