package com.texttosql.backend.service;

import com.texttosql.backend.dto.auth.AuthResponse;
import com.texttosql.backend.dto.auth.LoginRequest;
import com.texttosql.backend.dto.auth.RegisterRequest;
import com.texttosql.backend.dto.auth.UserInfoResponse;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.AuthenticationException;
import com.texttosql.backend.exception.DuplicateResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.util.JwtTokenUtil;
import com.texttosql.backend.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserContext userContext;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil, UserContext userContext) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userContext = userContext;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed: username already exists - {}", request.username());
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: email already exists - {}", request.email());
            throw new DuplicateResourceException("Email '" + request.email() + "' is already registered");
        }

        UserEntity user = UserEntity.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {} (ID: {})", user.getUsername(), user.getUserId());

        // Generate JWT token
        String token = jwtTokenUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getEmail()
        );

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                jwtTokenUtil.getExpirationDate().toInstant(),
                ZoneId.systemDefault()
        );

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.email());

        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", request.email());
                    return new AuthenticationException("Invalid username/email or password");
                });

        if (!user.getActive()) {
            log.warn("Login failed: account deactivated - {}", user.getUsername());
            throw new AuthenticationException("Account is deactivated.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed: incorrect password - {}", user.getUsername());
            throw new AuthenticationException("Invalid email or password");
        }

        log.info("User logged in successfully: {} (ID: {})", user.getUsername(), user.getUserId());

        String token = jwtTokenUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getEmail()
        );

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                jwtTokenUtil.getExpirationDate().toInstant(),
                ZoneId.systemDefault()
        );

        return new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser() {
        UUID userId = userContext.getCurrentUserId();

        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserInfoResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getActive(),
                user.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public boolean isUserActiveById(UUID userId) {
        return userRepository.isActive(userId);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserById(UUID userId) {
        return userRepository.findByUserIdAndActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}