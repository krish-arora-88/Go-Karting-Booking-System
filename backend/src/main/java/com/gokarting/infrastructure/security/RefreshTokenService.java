package com.gokarting.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Manages refresh tokens stored in Redis.
 *
 * Key strategy:
 *   "refresh:{tokenHash}" → username
 *
 * On each /auth/refresh call:
 *   1. Validate token exists in Redis
 *   2. Delete old token (rotation — old token can never be reused)
 *   3. Issue new access token + new refresh token
 *
 * This prevents refresh token theft: if an attacker uses a stolen token,
 * the legitimate user's next refresh will fail (token already deleted).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final String PREFIX = "refresh:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    @Value("${app.jwt.refresh-token-expiry-seconds}")
    private long refreshTokenExpirySeconds;

    public String createRefreshToken(String username) {
        String token = generateSecureToken();
        redis.opsForValue().set(
                PREFIX + token,
                username,
                Duration.ofSeconds(refreshTokenExpirySeconds)
        );
        log.debug("Refresh token created for user={}", username);
        return token;
    }

    /**
     * Validates and rotates the refresh token in one atomic step.
     * Returns the username if valid, empty if token not found / expired.
     */
    public Optional<String> rotateRefreshToken(String oldToken, String newToken) {
        String key = PREFIX + oldToken;
        String username = redis.opsForValue().get(key);
        if (username == null) {
            return Optional.empty();
        }
        redis.delete(key);
        redis.opsForValue().set(
                PREFIX + newToken,
                username,
                Duration.ofSeconds(refreshTokenExpirySeconds)
        );
        log.debug("Refresh token rotated for user={}", username);
        return Optional.of(username);
    }

    public void revokeRefreshToken(String token) {
        redis.delete(PREFIX + token);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
