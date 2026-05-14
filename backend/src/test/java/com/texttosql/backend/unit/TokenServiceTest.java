package com.texttosql.backend.unit;

import com.texttosql.backend.entity.TokenEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.enums.TokenType;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.exception.TokenAlreadyUsedException;
import com.texttosql.backend.exception.TokenExpiredException;
import com.texttosql.backend.repository.TokenRepository;
import com.texttosql.backend.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import java.time.Duration;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "emailVerificationExpiryTime", Duration.ofDays(1));
        ReflectionTestUtils.setField(tokenService, "passwordResetExpiryTime", Duration.ofHours(1));
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiryTime", 2592000000L);
    }

    @Test
    void createVerificationToken_ShouldReturnToken_WhenUserIsValid() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        when(tokenRepository.save(any(TokenEntity.class)))
                .thenAnswer(invocation -> {
                    TokenEntity tokenEntity = invocation.getArgument(0);
                    tokenEntity.setTokenId(UUID.randomUUID());
                    return tokenEntity;
                });

        String token = tokenService.createEmailVerificationToken(userEntity);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        verify(tokenRepository).save(any(TokenEntity.class));
    }

    @Test
    void validateVerificationToken_ShouldReturnUser_WhenTokenIsValid() {
        String tokenValue = "valid-verification-token";
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        TokenEntity verificationToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .user(userEntity)
                .type(TokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.VERIFICATION))
                .thenReturn(Optional.of(verificationToken));

        UserEntity result = tokenService.validateToken(tokenValue, TokenType.VERIFICATION);

        assertThat(result).isEqualTo(userEntity);
        verify(tokenRepository).findByTokenAndType(tokenValue, TokenType.VERIFICATION);
    }

    @Test
    void validateVerificationToken_ShouldThrowException_WhenTokenExpired() {
        String tokenValue = "expired-token";

        TokenEntity verificationToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .type(TokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.VERIFICATION))
                .thenReturn(Optional.of(verificationToken));

        assertThatThrownBy(() -> tokenService.validateToken(tokenValue, TokenType.VERIFICATION))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateVerificationToken_ShouldThrowException_WhenTokenAlreadyUsed() {
        String tokenValue = "used-token";

        TokenEntity verificationToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .type(TokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(true)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.VERIFICATION))
                .thenReturn(Optional.of(verificationToken));

        assertThatThrownBy(() -> tokenService.validateToken(tokenValue, TokenType.VERIFICATION))
                .isInstanceOf(TokenAlreadyUsedException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void validateVerificationToken_ShouldThrowException_WhenTokenNotFound() {
        String tokenValue = "nonexistent-token";

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.VERIFICATION))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validateToken(tokenValue, TokenType.VERIFICATION))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void markVerificationTokenAsUsed_ShouldMarkTokenAsUsed() {
        String tokenValue = "valid-token";
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        TokenEntity verificationToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .user(userEntity)
                .type(TokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.VERIFICATION))
                .thenReturn(Optional.of(verificationToken));
        when(tokenRepository.save(any(TokenEntity.class)))
                .thenReturn(verificationToken);

        tokenService.markTokenAsUsed(tokenValue, TokenType.VERIFICATION);

        assertThat(verificationToken.getUsed()).isTrue();
        verify(tokenRepository).save(verificationToken);
    }

    @Test
    void validateVerificationToken_ShouldCheckIsExpiredMethod() {
        String tokenValue = "test-token";
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        TokenEntity verificationToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .user(userEntity)
                .type(TokenType.VERIFICATION)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .used(false)
                .build();

        assertThat(verificationToken.isExpired()).isTrue();
    }

    @Test
    void createPasswordResetToken_ShouldReturnToken_WhenUserIsValid() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        when(tokenRepository.save(any(TokenEntity.class)))
                .thenAnswer(invocation -> {
                    TokenEntity token = invocation.getArgument(0);
                    token.setTokenId(UUID.randomUUID());
                    return token;
                });

        String token = tokenService.createPasswordResetToken(userEntity);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        verify(tokenRepository).save(any(TokenEntity.class));
    }

    @Test
    void validatePasswordResetToken_ShouldReturnUser_WhenTokenIsValid() {
        String tokenValue = "valid-reset-token";
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        TokenEntity resetToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .user(userEntity)
                .type(TokenType.PASSWORD)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.PASSWORD))
                .thenReturn(Optional.of(resetToken));

        UserEntity result = tokenService.validateToken(tokenValue, TokenType.PASSWORD);

        assertThat(result).isEqualTo(userEntity);
        verify(tokenRepository).findByTokenAndType(tokenValue, TokenType.PASSWORD);
    }

    @Test
    void validatePasswordResetToken_ShouldThrowException_WhenTokenExpired() {
        String tokenValue = "expired-reset-token";

        TokenEntity resetToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .type(TokenType.PASSWORD)
                .expiresAt(LocalDateTime.now().minusMinutes(30))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.PASSWORD))
                .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> tokenService.validateToken(tokenValue, TokenType.PASSWORD))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validatePasswordResetToken_ShouldThrowException_WhenTokenAlreadyUsed() {
        String tokenValue = "used-reset-token";

        TokenEntity resetToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .type(TokenType.PASSWORD)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(true)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.PASSWORD))
                .thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> tokenService.validateToken(tokenValue, TokenType.PASSWORD))
                .isInstanceOf(TokenAlreadyUsedException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void markPasswordResetTokenAsUsed_ShouldMarkTokenAsUsed() {
        String tokenValue = "valid-reset-token";

        TokenEntity resetToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .type(TokenType.PASSWORD)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.PASSWORD))
                .thenReturn(Optional.of(resetToken));
        when(tokenRepository.save(any(TokenEntity.class)))
                .thenReturn(resetToken);

        tokenService.markTokenAsUsed(tokenValue, TokenType.PASSWORD);

        assertThat(resetToken.getUsed()).isTrue();
        verify(tokenRepository).save(resetToken);
    }

    @Test
    void validatePasswordResetToken_ShouldThrowException_WhenTokenNotFound() {
        String tokenValue = "nonexistent-reset-token";

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.PASSWORD))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.validateToken(tokenValue, TokenType.PASSWORD))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void createRefreshToken_ShouldReturnToken_WhenUserIsValid() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        when(tokenRepository.save(any(TokenEntity.class)))
                .thenAnswer(invocation -> {
                    TokenEntity token = invocation.getArgument(0);
                    token.setTokenId(UUID.randomUUID());
                    return token;
                });

        String token = tokenService.createRefreshToken(userEntity);

        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        verify(tokenRepository).save(any(TokenEntity.class));
    }

    @Test
    void validateRefreshToken_ShouldReturnUser_WhenTokenIsValid() {
        String tokenValue = "valid-refresh-token";
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .build();

        TokenEntity refreshToken = TokenEntity.builder()
                .tokenId(UUID.randomUUID())
                .token(tokenValue)
                .user(userEntity)
                .type(TokenType.REFRESH)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .used(false)
                .build();

        when(tokenRepository.findByTokenAndType(tokenValue, TokenType.REFRESH))
                .thenReturn(Optional.of(refreshToken));

        UserEntity result = tokenService.validateToken(tokenValue, TokenType.REFRESH);

        assertThat(result).isEqualTo(userEntity);
        verify(tokenRepository).findByTokenAndType(tokenValue, TokenType.REFRESH);
    }
}