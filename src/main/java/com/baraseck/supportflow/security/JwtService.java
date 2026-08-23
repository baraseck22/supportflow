package com.baraseck.supportflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;
    private final Clock clock;
    public JwtService(@Value("${supportflow.jwt.secret}") String secret,
                      @Value("${supportflow.jwt.expiration-seconds:3600}") long expirationSeconds, Clock clock) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalArgumentException("JWT_SECRET doit contenir au moins 32 octets");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
        this.clock = clock;
    }
    public String generate(SupportFlowPrincipal principal) {
        Instant now = clock.instant();
        return Jwts.builder().subject(principal.id().toString()).claim("email", principal.email())
                .claim("role", principal.role().name()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build().parseSignedClaims(token).getPayload(); }
    public long expirationSeconds() { return expirationSeconds; }
}
