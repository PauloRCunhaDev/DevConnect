package com.devconnect.api_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final Key signingKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtUtil(
            @Value("${security.jwt.secret}") String jwtSecret,
            @Value("${security.jwt.expiration-ms}") long accessExpirationMs,
            @Value("${security.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateAccessToken(String subject) {
        return buildToken(subject, ACCESS_TOKEN_TYPE, accessExpirationMs);
    }

    public String generateRefreshToken(String subject) {
        return buildToken(subject, REFRESH_TOKEN_TYPE, refreshExpirationMs);
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValidForType(token, ACCESS_TOKEN_TYPE);
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValidForType(token, REFRESH_TOKEN_TYPE);
    }

    private String buildToken(String subject, String tokenType, long expirationMs) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(subject)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    private boolean isTokenValidForType(String token, String expectedTokenType) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return expectedTokenType.equals(tokenType);
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
