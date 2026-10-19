package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        validateAndInitializeKey(jwtProperties.getSecretBase64());
    }

    public void validateAndInitializeKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.trim().isEmpty()) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 is required and cannot be empty");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretBase64.trim());
        } catch (Exception e) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 must be a valid Base64 string", e);
        }

        // Al menos 32 bytes (256 bits) para HS256
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 must contain at least 32 bytes (256 bits). Found: " + keyBytes.length + " bytes"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null when generating JWT");
        }

        Instant now = Instant.now();
        long ttlSeconds = jwtProperties.getTtlSeconds();

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parseSignedClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token.trim());

        // Verificar explícitamente algoritmo HS256
        String alg = claimsJws.getHeader().getAlgorithm();
        if (alg == null || !alg.equalsIgnoreCase("HS256")) {
            throw new JwtException("Unexpected JWT signing algorithm: " + alg);
        }

        // Verificar subject no nulo ni vacío
        String subject = claimsJws.getPayload().getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new JwtException("JWT subject (sub) is missing or empty");
        }

        return claimsJws;
    }

    public Claims extractClaims(String token) {
        return parseSignedClaims(token).getPayload();
    }

    public Long extractUserId(String token) {
        String subject = extractClaims(token).getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new JwtException("JWT subject is not a valid user ID: " + subject, e);
        }
    }

    public boolean validateToken(String token) {
        try {
            parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException | UnsupportedJwtException | SecurityException | IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public long getExpiresInSeconds() {
        return jwtProperties.getTtlSeconds();
    }
}
