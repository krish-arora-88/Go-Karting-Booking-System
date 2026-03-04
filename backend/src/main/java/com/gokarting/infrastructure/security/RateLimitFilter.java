package com.gokarting.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed rate limiting via Bucket4j + Redis.
 * Each IP gets its own token bucket stored in Redis — works correctly
 * across multiple application instances (horizontal scale).
 *
 * Limits:
 *   - Auth endpoints: 5 req / 15 min per IP (brute-force protection)
 *   - Other endpoints: governed by per-user limits enforced at service layer
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final LettuceBasedProxyManager<String> proxyManager;
    private final int authRequestsPerWindow;
    private final int authWindowMinutes;

    public RateLimitFilter(
            LettuceBasedProxyManager<String> proxyManager,
            @Value("${app.rate-limit.auth-requests-per-window}") int authRequestsPerWindow,
            @Value("${app.rate-limit.auth-window-minutes}") int authWindowMinutes) {
        this.proxyManager = proxyManager;
        this.authRequestsPerWindow = authRequestsPerWindow;
        this.authWindowMinutes = authWindowMinutes;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/v1/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String bucketKey = "rate_limit:auth:" + clientIp;

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(authRequestsPerWindow)
                        .refillIntervally(authRequestsPerWindow, Duration.ofMinutes(authWindowMinutes))
                        .build())
                .build();

        var bucket = proxyManager.builder().build(bucketKey, configSupplier);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP={} on auth endpoint", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                    {"type":"https://gokarting.api/problems/rate-limit-exceeded",
                     "title":"Too Many Requests",
                     "status":429,
                     "detail":"Auth rate limit exceeded. Try again later."}
                    """);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
