package com.gokarting.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injects a traceId into MDC for every request.
 * All log statements in the same request thread will include this ID,
 * making distributed debugging possible without a full tracing backend.
 *
 * Output (JSON via logstash-logback-encoder):
 *   {"traceId":"abc123","userId":"racer42","requestPath":"/api/v1/bookings","message":"..."}
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        try {
            String traceId = request.getHeader("X-Trace-Id");
            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }

            MDC.put("traceId", traceId);
            MDC.put("requestPath", request.getRequestURI());
            MDC.put("method", request.getMethod());

            // userId injected after JWT filter runs (next request processing cycle)
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String username) {
                MDC.put("userId", username);
            }

            response.setHeader("X-Trace-Id", traceId);
            chain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}
