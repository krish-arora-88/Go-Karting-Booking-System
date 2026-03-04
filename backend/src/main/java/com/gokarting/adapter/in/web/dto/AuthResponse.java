package com.gokarting.adapter.in.web.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs,
        String username,
        String role
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  long expiresInMs, String username, String role) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs, username, role);
    }
}
