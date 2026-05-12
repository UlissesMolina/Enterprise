package com.example.expense_api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(SECRET, 3600000); // 1 hour
    }

    @Test
    void generateToken_returnsNonNull() {
        String token = tokenProvider.generateToken("user@test.com");
        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    void getEmailFromToken_returnsCorrectEmail() {
        String token = tokenProvider.generateToken("user@test.com");
        String email = tokenProvider.getEmailFromToken(token);
        assertThat(email).isEqualTo("user@test.com");
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = tokenProvider.generateToken("user@test.com");
        assertThat(tokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, -1000); // already expired
        String token = shortLived.generateToken("user@test.com");
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = tokenProvider.generateToken("user@test.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertThat(tokenProvider.validateToken(null)).isFalse();
    }

    @Test
    void validateToken_differentSecret_returnsFalse() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "different-secret-key-that-is-at-least-32-chars", 3600000);
        String token = otherProvider.generateToken("user@test.com");
        assertThat(tokenProvider.validateToken(token)).isFalse();
    }
}
