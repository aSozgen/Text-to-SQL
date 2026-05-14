package com.texttosql.backend.controller;

import com.texttosql.backend.dto.auth.*;
import com.texttosql.backend.dto.entity.UserDto;
import com.texttosql.backend.service.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication", description = "User registration and authentication operations.")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/guest-token")
    public ResponseEntity<AuthenticationResponse> getGuestToken(jakarta.servlet.http.HttpServletResponse response) {
        return ResponseEntity.ok(authenticationService.generateGuestToken(response));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        authenticationService.register(registerRequest, token);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authenticationService.login(loginRequest, response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            throw new IllegalArgumentException("Refresh token is missing");
        }
        return ResponseEntity.ok(authenticationService.refreshToken(refreshToken, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authenticationService.logout(refreshToken, response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<AuthenticationResponse> validate(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = extractToken(authHeader);
        return ResponseEntity.ok(authenticationService.validateToken(token));
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        String token = extractToken(authHeader);
        authenticationService.updateProfile(token, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        String token = extractToken(authHeader);
        authenticationService.changePassword(token, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteAccount(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = extractToken(authHeader);
        authenticationService.deleteAccount(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(
            @RequestHeader("Authorization") String authHeader
    ) {

        String token = extractToken(authHeader);
        return ResponseEntity.ok(authenticationService.getMe(token));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is running.");
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authenticationService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(
            @Valid @RequestBody EmailRequest request
    ) {
        authenticationService.resendVerificationEmail(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody EmailRequest request
    ) {
        authenticationService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authenticationService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }
}
