package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET_BASE64 = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI="; // 32 bytes
    private static final String ISSUER = "pulsepass-api";

    private JwtProperties properties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setIssuer(ISSUER);
        properties.setSecretBase64(VALID_SECRET_BASE64);
        properties.setAccessTokenTtl(Duration.ofMinutes(15));

        jwtService = new JwtService(properties);
        jwtService.init();
    }

    @Test
    @DisplayName("Firma correcta y generación válida de JWT con claims obligatorios")
    void shouldGenerateValidTokenWithRequiredClaims() {
        Long userId = 42L;

        String token = jwtService.generateToken(userId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);

        Claims claims = jwtService.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("Token expirado debe ser rechazado")
    void shouldRejectExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(VALID_SECRET_BASE64));
        Instant now = Instant.now();

        String expiredToken = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
        assertThatThrownBy(() -> jwtService.extractClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Token con issuer incorrecto debe ser rechazado")
    void shouldRejectTokenWithWrongIssuer() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(VALID_SECRET_BASE64));
        Instant now = Instant.now();

        String wrongIssuerToken = Jwts.builder()
                .subject("42")
                .issuer("malicious-issuer")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(jwtService.validateToken(wrongIssuerToken)).isFalse();
        assertThatThrownBy(() -> jwtService.extractClaims(wrongIssuerToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token con firma alterada (manipulación) debe ser rechazado")
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(42L);
        // Alterar el último carácter del token
        char lastChar = token.charAt(token.length() - 1);
        char alteredChar = (lastChar == 'a') ? 'b' : 'a';
        String tamperedToken = token.substring(0, token.length() - 1) + alteredChar;

        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
        assertThatThrownBy(() -> jwtService.extractClaims(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token nulo, vacío o malformado debe ser rechazado")
    void shouldRejectEmptyOrMalformedToken() {
        assertThat(jwtService.validateToken("")).isFalse();
        assertThat(jwtService.validateToken("   ")).isFalse();
        assertThat(jwtService.validateToken("not.a.valid.jwt")).isFalse();

        assertThatThrownBy(() -> jwtService.extractClaims(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtService.extractClaims("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Clave ausente, nula o débil (< 32 bytes) debe fallar al inicializar")
    void shouldFailInitializationWhenSecretIsWeakOrAbsent() {
        JwtProperties emptyProps = new JwtProperties();
        emptyProps.setSecretBase64("");
        JwtService serviceWithEmptySecret = new JwtService(emptyProps);

        assertThatThrownBy(serviceWithEmptySecret::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEPASS_JWT_SECRET_BASE64 is required");

        // Clave de solo 16 bytes en Base64
        JwtProperties weakProps = new JwtProperties();
        weakProps.setSecretBase64("MTIzNDU2Nzg5MDEyMzQ1Ng=="); // 16 bytes
        JwtService serviceWithWeakSecret = new JwtService(weakProps);

        assertThatThrownBy(serviceWithWeakSecret::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
