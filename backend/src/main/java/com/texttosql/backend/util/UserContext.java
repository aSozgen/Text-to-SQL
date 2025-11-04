package com.texttosql.backend.util;

import com.texttosql.backend.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
@Slf4j
public class UserContext {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    public UUID getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        String userIdHeader = request.getHeader(USER_ID_HEADER);

        if (userIdHeader == null || userIdHeader.isEmpty()) {
            log.error("Missing X-User-Id header in authenticated request");
            throw new UnauthorizedException("User authentication required");
        }

        try {
            return UUID.fromString(userIdHeader);
        } catch (IllegalArgumentException e) {
            log.error("Invalid UUID format in X-User-Id header: {}", userIdHeader);
            throw new UnauthorizedException("Invalid user ID format");
        }
    }

    public String getCurrentUsername() {
        HttpServletRequest request = getCurrentRequest();
        String username = request.getHeader(USERNAME_HEADER);

        if (username == null || username.isEmpty()) {
            log.error("Missing X-Username header in authenticated request");
            throw new UnauthorizedException("User authentication required");
        }

        return username;
    }

    public boolean isAuthenticated() {
        try {
            HttpServletRequest request = getCurrentRequest();
            return request.getHeader(USER_ID_HEADER) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new IllegalStateException("No request context available");
        }

        return attributes.getRequest();
    }
}
