package com.texttosql.backend.service;

import com.texttosql.backend.entity.TokenEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.enums.TokenType;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.exception.TokenAlreadyUsedException;
import com.texttosql.backend.exception.TokenExpiredException;
import com.texttosql.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String createVerificationToken(UserEntity user) {
        // Invalidate any existing unused tokens
        tokenRepository.findByUserAndTypeAndUsedFalse(user, TokenType.VERIFICATION)
                .ifPresent(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });

        String token = generateSecureToken();
        TokenEntity verificationToken = TokenEntity.builder()
                .token(token)
                .user(user)
                .type(TokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public String createPasswordResetToken(UserEntity user) {
        // Invalidate any existing unused tokens
        tokenRepository.findByUserAndTypeAndUsedFalse(user, TokenType.PASSWORD)
                .ifPresent(token -> {
                    token.setUsed(true);
                    tokenRepository.save(token);
                });

        String token = generateSecureToken();
        TokenEntity resetToken = TokenEntity.builder()
                .token(token)
                .user(user)
                .type(TokenType.PASSWORD)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        tokenRepository.save(resetToken);
        return token;
    }

    @Transactional(readOnly = true)
    public UserEntity validateVerificationToken(String token) {
        TokenEntity verificationToken = tokenRepository.findByTokenAndType(token, TokenType.VERIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (verificationToken.getUsed()) {
            throw new TokenAlreadyUsedException("Verification token has already been used.");
        }

        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("Verification token has expired.");
        }

        return verificationToken.getUser();
    }

    @Transactional(readOnly = true)
    public UserEntity validatePasswordResetToken(String token) {
        TokenEntity resetToken = tokenRepository.findByTokenAndType(token, TokenType.PASSWORD)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid password reset token"));

        if (resetToken.getUsed()) {
            throw new TokenAlreadyUsedException("Password reset token has already been used.");
        }

        if (resetToken.isExpired()) {
            throw new TokenExpiredException("Password reset token has expired.");
        }

        return resetToken.getUser();
    }

    @Transactional
    public void markVerificationTokenAsUsed(String token) {
        TokenEntity verificationToken = tokenRepository.findByTokenAndType(token, TokenType.VERIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
    }

    @Transactional
    public void markPasswordResetTokenAsUsed(String token) {
        TokenEntity resetToken = tokenRepository.findByTokenAndType(token, TokenType.PASSWORD)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    // Clean up expired tokens daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteByExpiresAtBeforeAndType(now, TokenType.VERIFICATION);
        tokenRepository.deleteByExpiresAtBeforeAndType(now, TokenType.PASSWORD);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
