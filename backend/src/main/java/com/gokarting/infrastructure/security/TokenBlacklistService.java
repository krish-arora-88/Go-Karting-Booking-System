package com.gokarting.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * On logout, the access token's JTI is added to a Redis blacklist
 * with a TTL matching the token's remaining lifetime.
 * JwtAuthFilter checks this before allowing any request through.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String PREFIX = "blacklist:";
    private final StringRedisTemplate redis;

    public void blacklist(String jti, long tokenExpiryMs) {
        long remainingMs = tokenExpiryMs - System.currentTimeMillis();
        if (remainingMs > 0) {
            redis.opsForValue().set(PREFIX + jti, "1", Duration.ofMillis(remainingMs));
        }
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + jti));
    }
}
