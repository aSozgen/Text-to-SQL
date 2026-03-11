package com.texttosql.backend.service;

import com.texttosql.backend.entity.PasswordResetTokenEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.VerificationTokenEntity;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.exception.TokenAlreadyUsedException;
import com.texttosql.backend.exception.TokenExpiredException;
import com.texttosql.backend.repository.PasswordResetTokenRepository;
import com.texttosql.backend.repository.VerificationTokenRepository;
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

    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String createVerificationToken(UserEntity user) {
        // Invalidate any existing unused tokens
        verificationTokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(token -> {
                    token.setUsed(true);
                    verificationTokenRepository.save(token);
                });

        String token = generateSecureToken();
        VerificationTokenEntity verificationToken = VerificationTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        verificationTokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public String createPasswordResetToken(UserEntity user) {
        // Invalidate any existing unused tokens
        passwordResetTokenRepository.findByUserAndUsedFalse(user)
                .ifPresent(token -> {
                    token.setUsed(true);
                    passwordResetTokenRepository.save(token);
                });

        String token = generateSecureToken();
        PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    @Transactional(readOnly = true)
    public UserEntity validateVerificationToken(String token) {
        VerificationTokenEntity verificationToken = verificationTokenRepository.findByToken(token)
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
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByToken(token)
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
        VerificationTokenEntity verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);
    }

    @Transactional
    public void markPasswordResetTokenAsUsed(String token) {
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // Clean up expired tokens daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        verificationTokenRepository.deleteByExpiresAtBefore(now);
        passwordResetTokenRepository.deleteByExpiresAtBefore(now);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
