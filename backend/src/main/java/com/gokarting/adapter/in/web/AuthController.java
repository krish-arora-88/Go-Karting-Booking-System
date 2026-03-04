package com.gokarting.adapter.in.web;

import com.gokarting.adapter.in.web.dto.AuthResponse;
import com.gokarting.adapter.in.web.dto.LoginRequest;
import com.gokarting.adapter.in.web.dto.RegisterRequest;
import com.gokarting.domain.port.in.RegisterUserUseCase;
import com.gokarting.domain.port.out.UserRepository;
import com.gokarting.infrastructure.security.JwtService;
import com.gokarting.infrastructure.security.RefreshTokenService;
import com.gokarting.infrastructure.security.TokenBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, and logout")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService blacklistService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "409", description = "Username already taken")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        var user = registerUserUseCase.register(
                new RegisterUserUseCase.RegisterCommand(req.username(), req.email(), req.password()));

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.of(accessToken, refreshToken,
                jwtService.extractExpiryMs(accessToken), user.getUsername(), user.getRole().name());
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive access + refresh tokens")
    @ApiResponse(responseCode = "200", description = "Tokens issued")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        var user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.of(accessToken, refreshToken,
                jwtService.extractExpiryMs(accessToken), user.getUsername(), user.getRole().name());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and get new access token",
               description = "Implements refresh token rotation — the old refresh token is immediately invalidated.")
    @ApiResponse(responseCode = "200", description = "New tokens issued")
    @ApiResponse(responseCode = "401", description = "Refresh token invalid or expired")
    public AuthResponse refresh(@RequestBody Map<String, String> body) {
        String oldRefreshToken = body.get("refreshToken");
        if (oldRefreshToken == null) {
            throw new BadCredentialsException("refreshToken field required");
        }

        String newRefreshToken = refreshTokenService.createRefreshToken("__tmp__");
        String username = refreshTokenService.rotateRefreshToken(oldRefreshToken, newRefreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        // Update the new token to the correct username (was set to __tmp__ above)
        refreshTokenService.revokeRefreshToken(newRefreshToken);
        newRefreshToken = refreshTokenService.createRefreshToken(username);

        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String accessToken = jwtService.generateAccessToken(username, user.getRole().name());
        return AuthResponse.of(accessToken, newRefreshToken,
                jwtService.extractExpiryMs(accessToken), username, user.getRole().name());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout — blacklists current access token and revokes refresh token")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public void logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) Map<String, String> body) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                blacklistService.blacklist(jwtService.extractJti(token),
                        jwtService.extractExpiryMs(token));
            }
        }

        if (body != null && body.containsKey("refreshToken")) {
            refreshTokenService.revokeRefreshToken(body.get("refreshToken"));
        }
    }
}
