package com.texttosql.backend.dto.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserInfoResponse(
        UUID userId,
        String username,
        String email,
        Boolean active,
        LocalDateTime createdAt
) {}