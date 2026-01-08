package com.texttosql.backend.util;

import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        throw new ResourceNotFoundException("No authenticated user found");
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUsername();
        }

        throw new ResourceNotFoundException("No authenticated user found");
    }

    public CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }

        throw new ResourceNotFoundException("No authenticated user found");
    }

    public Optional<UserEntity> getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userRepository.findByUserId(userDetails.getUserId());
        }

        throw new ResourceNotFoundException("No authenticated user found");
    }

    public boolean hasRole(RoleEnum role) {
        CustomUserDetails currentUser = getCurrentUser();
        return currentUser.getRole().equals(role);
    }
}