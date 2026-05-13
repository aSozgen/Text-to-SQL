package com.texttosql.backend.service;

import com.texttosql.backend.entity.TokenEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.enums.TokenType;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.exception.TokenAlreadyUsedException;
import com.texttosql.backend.exception.TokenExpiredException;
import com.texttosql.backend.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value( "${token.email-verification.expiration}")
    private Duration emailVerificationExpiryTime;
    @Value( "${token.password-reset.expiration}")
    private Duration passwordResetExpiryTime;
    @Value( "${token.jwt.refresh.expiration}")
    private Duration refreshTokenExpiryTime;

    @Transactional
    public String createEmailVerificationToken(UserEntity user) {
        // Invalidate any existing unused tokens
        invalidateToken(user, TokenType.VERIFICATION);

        TokenEntity verificationToken = new TokenEntity();
        String token = createToken(verificationToken, user, TokenType.VERIFICATION, emailVerificationExpiryTime);

        tokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public String createPasswordResetToken(UserEntity user) {
        // Invalidate any existing unused tokens
        invalidateToken(user, TokenType.PASSWORD);

        TokenEntity resetToken = new TokenEntity();
        String token = createToken(resetToken, user, TokenType.PASSWORD, passwordResetExpiryTime);

        tokenRepository.save(resetToken);
        return token;
    }

    @Transactional
    public String createRefreshToken(UserEntity user) {
        TokenEntity refreshToken = new TokenEntity();
        String token = createToken(refreshToken, user, TokenType.REFRESH, refreshTokenExpiryTime);

        tokenRepository.save(refreshToken);
        return token;
    }

    private String createToken(TokenEntity tokenEntity, UserEntity user, TokenType type, Duration expiryTime) {
        String token = generateSecureToken();
        tokenEntity.setToken(token);
        tokenEntity.setUser(user);
        tokenEntity.setType(type);
        tokenEntity.setExpiresAt(LocalDateTime.now().plus(expiryTime));
        tokenEntity.setUsed(false);
        return token;
    }

    @Transactional(readOnly = true)
    public UserEntity validateToken(String token, TokenType type) {
        TokenEntity tokenEntity = tokenRepository.findByTokenAndType(token, type)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid token."));

        if (tokenEntity.getUsed()) {
            throw new TokenAlreadyUsedException("Token has already been used.");
        }

        if (tokenEntity.isExpired()) {
            throw new TokenExpiredException("Token has expired.");
        }

        return tokenEntity.getUser();
    }

    @Transactional
    public void markTokenAsUsed(String token, TokenType type) {
        TokenEntity tokenEntity = tokenRepository.findByTokenAndType(token, type)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));
        tokenEntity.setUsed(true);
        tokenRepository.save(tokenEntity);
    }

    private void invalidateToken(UserEntity user, TokenType type) {
        List<TokenEntity> tokens = tokenRepository.findAllByUserAndTypeAndUsedFalse(user, type);
        tokens.forEach(t -> t.setUsed(true));
        tokenRepository.saveAll(tokens);
    }

    // Clean up expired tokens daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
