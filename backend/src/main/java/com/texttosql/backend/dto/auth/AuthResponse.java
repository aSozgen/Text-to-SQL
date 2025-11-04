package com.texttosql.backend.dto.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID userId,
        String username,
        String email,
        LocalDateTime expiresAt
) {
    public AuthResponse(String token, UUID userId, String username, String email, LocalDateTime expiresAt) {
        this(token, "Bearer", userId, username, email, expiresAt);
    }
}