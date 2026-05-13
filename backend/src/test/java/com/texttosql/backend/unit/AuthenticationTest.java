package com.texttosql.backend.unit;

import com.texttosql.backend.dto.auth.*;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.enums.TokenType;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.EmailNotVerifiedException;
import com.texttosql.backend.exception.TokenExpiredException;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.AuthenticationService;
import com.texttosql.backend.service.SchemaService;
import com.texttosql.backend.service.TokenService;
import com.texttosql.backend.service.EmailService;
import com.texttosql.backend.util.JwtUtil;
import com.texttosql.backend.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationTest {

    private final UserMapper realMapper = Mappers.getMapper(UserMapper.class);

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenService tokenService;
    @Mock
    private EmailService emailService;
    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_ShouldReturnVoid_WhenRequestIsValid() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "password");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");
        when(tokenService.createEmailVerificationToken(any(UserEntity.class))).thenReturn("token123");

        authenticationService.register(request, null);

        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).createEmailVerificationToken(any(UserEntity.class));
        verify(emailService).sendVerificationEmail(eq(request.email()), eq("token123"));
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest("newUser", "existing@test.com", "password");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request, null))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreCorrect() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .emailVerified(true)
                .active(true)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmailAndActiveTrue(loginRequest.email())).thenReturn(Optional.of(userEntity));
        when(userMapper.toDetails(any(UserEntity.class))).thenAnswer(i -> {
            UserEntity entity = i.getArgument(0);
            CustomUserDetails mockDetails = new CustomUserDetails();
            mockDetails.setUsername(entity.getUsername());
            mockDetails.setEmail(entity.getEmail());
            mockDetails.setRole(entity.getRole());
            return mockDetails;
        });
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("jwt-token");
        when(tokenService.createRefreshToken(any(UserEntity.class))).thenReturn("refresh-token");

        AuthenticationResponse response = authenticationService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(userRepository).findByEmailAndActiveTrue(loginRequest.email());
    }

    @Test
    void login_ShouldThrowException_WhenEmailNotVerified() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password");
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .emailVerified(false)
                .active(true)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmailAndActiveTrue(loginRequest.email())).thenReturn(Optional.of(userEntity));

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Email not verified");

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFoundAfterAuth() {
        LoginRequest loginRequest = new LoginRequest("ghost", "password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmailAndActiveTrue("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found.");
    }

    @Test
    void login_ShouldPropagateException_WhenAuthenticationFails() {
        LoginRequest loginRequest = new LoginRequest("wrongUser", "wrongPass");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmailAndActiveTrue(any());
    }

    @Test
    void verifyEmail_ShouldMarkEmailAsVerified_WhenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .emailVerified(false)
                .active(true)
                .build();

        when(tokenService.validateToken("valid-token", TokenType.VERIFICATION)).thenReturn(userEntity);

        authenticationService.verifyEmail("valid-token");

        verify(tokenService).validateToken("valid-token", TokenType.VERIFICATION);
        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).markTokenAsUsed("valid-token", TokenType.VERIFICATION);
        verify(schemaService).copyTemplatesToUser(userMapper.toDetails(userEntity));
    }

    @Test
    void verifyEmail_ShouldThrowException_WhenTokenIsExpired() {
        when(tokenService.validateToken("expired-token", TokenType.VERIFICATION))
                .thenThrow(new TokenExpiredException("Verification token has expired"));

        assertThatThrownBy(() -> authenticationService.verifyEmail("expired-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void generateGuestToken_ShouldReturnValidToken() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-pass");
        UserEntity userEntity = UserEntity.builder()
                .username("Guest")
                .email("guest@texttosql.local.com")
                .role(Role.GUEST)
                .emailVerified(true)
                .active(true)
                .build();
        when(userMapper.toDetails(any(UserEntity.class))).thenAnswer(i -> {
            UserEntity entity = i.getArgument(0);
            CustomUserDetails mockDetails = new CustomUserDetails();
            mockDetails.setUsername(entity.getUsername());
            mockDetails.setEmail(entity.getEmail());
            mockDetails.setRole(entity.getRole());
            return mockDetails;
        });
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("guest-jwt-token");
        when(tokenService.createRefreshToken(any(UserEntity.class))).thenReturn("guest-refresh-token");

        AuthenticationResponse response = authenticationService.generateGuestToken();

        assertThat(response.getToken()).isEqualTo("guest-jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("guest-refresh-token");
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void register_ShouldConvertGuestToUser_WhenValidGuestTokenProvided() {
        RegisterRequest request = new RegisterRequest("test@example.com", "testuser", "password");
        UserEntity guestUser = UserEntity.builder()
                .username("Guest")
                .email("guest_123456@texttosql.local.com")
                .password("encoded")
                .role(Role.GUEST)
                .active(true)
                .emailVerified(true)
                .build();

        when(jwtUtil.validateToken("guest-token")).thenReturn(true);
        when(jwtUtil.extractEmail("guest-token")).thenReturn("guest_123456@texttosql.local.com");
        when(userRepository.findByEmailAndActiveTrue("guest_123456@texttosql.local.com")).thenReturn(Optional.of(guestUser));
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("new-encoded-pass");
        when(tokenService.createEmailVerificationToken(any(UserEntity.class))).thenReturn("new-verify-token");

        authenticationService.register(request, "guest-token");

        assertThat(guestUser.getUsername()).isEqualTo("testuser");
        assertThat(guestUser.getEmail()).isEqualTo("test@example.com");
        assertThat(guestUser.getRole()).isEqualTo(Role.USER);
        assertThat(guestUser.getEmailVerified()).isFalse();

        verify(userRepository).save(guestUser);
        verify(emailService).sendVerificationEmail("test@example.com", "new-verify-token");
    }

    @Test
    void resendVerificationEmail_ShouldSendNewToken_WhenEmailExists() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .emailVerified(false)
                .active(true)
                .build();

        EmailRequest request = new EmailRequest("test@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
        when(tokenService.createEmailVerificationToken(userEntity)).thenReturn("new-token");

        authenticationService.resendVerificationEmail(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(tokenService).createEmailVerificationToken(userEntity);
        verify(emailService).sendVerificationEmail("test@example.com", "new-token");
    }

    @Test
    void resendVerificationEmail_ShouldThrowException_WhenEmailAlreadyVerified() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .emailVerified(true)
                .active(true)
                .build();

        EmailRequest request = new EmailRequest("test@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));

        assertThatThrownBy(() -> authenticationService.resendVerificationEmail(request))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("already verified");

        verify(tokenService, never()).createEmailVerificationToken(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_ShouldThrowException_WhenUserNotFound() {
        EmailRequest request = new EmailRequest("nonexistent@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.resendVerificationEmail(request))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_ShouldSendResetEmail_WhenEmailExists() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .active(true)
                .build();

        EmailRequest request = new EmailRequest("test@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
        when(tokenService.createPasswordResetToken(userEntity)).thenReturn("reset-token");

        authenticationService.forgotPassword(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(tokenService).createPasswordResetToken(userEntity);
        verify(emailService).sendPasswordResetEmail("test@example.com", "reset-token");
    }

    @Test
    void forgotPassword_ShouldThrowException_WhenUserNotFound() {
        EmailRequest request = new EmailRequest("nonexistent@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.forgotPassword(request))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_ShouldUpdatePassword_WhenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        UserEntity userEntity = UserEntity.builder()
                .userId(userId)
                .username("testuser")
                .email("test@example.com")
                .password("oldEncodedPassword")
                .active(true)
                .build();

        ResetPasswordRequest request = new ResetPasswordRequest("valid-reset-token", "newPassword123");

        when(tokenService.validateToken("valid-reset-token", TokenType.PASSWORD)).thenReturn(userEntity);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        authenticationService.resetPassword(request);

        verify(tokenService).validateToken("valid-reset-token", TokenType.PASSWORD);
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).markTokenAsUsed("valid-reset-token", TokenType.PASSWORD);
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newPassword123");

        when(tokenService.validateToken("expired-token", TokenType.PASSWORD))
                .thenThrow(new TokenExpiredException("Reset token has expired"));

        assertThatThrownBy(() -> authenticationService.resetPassword(request))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsInvalid() {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newPassword123");

        when(tokenService.validateToken("invalid-token", TokenType.PASSWORD))
                .thenThrow(new TokenExpiredException("Invalid or expired reset token"));

        assertThatThrownBy(() -> authenticationService.resetPassword(request))
                .isInstanceOf(TokenExpiredException.class);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void validateToken_ShouldReturnResponse_WhenTokenIsValid() {
        String token = "valid-token";
        when(jwtUtil.validateToken(token)).thenReturn(true);

        AuthenticationResponse response = authenticationService.validateToken(token);

        assertThat(response.getToken()).isEqualTo(token);
    }

    @Test
    void validateToken_ShouldThrowException_WhenTokenIsInvalid() {
        String token = "invalid-token";
        when(jwtUtil.validateToken(token)).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.validateToken(token))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired token.");
    }

    @Test
    void deleteAccount_ShouldDeleteUser_WhenUserExists() {
        String token = "valid-token";
        String email = "test@example.com";

        UserEntity userEntity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .email(email)
                .active(true)
                .build();

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userRepository.findByEmailAndActiveTrue(email)).thenReturn(Optional.of(userEntity));

        authenticationService.deleteAccount(token);

        verify(userRepository).save(userEntity);
        assertThat(userEntity.getActive()).isFalse();
    }

    @Test
    void deleteAccount_ShouldThrowException_WhenUserNotFound() {
        String token = "valid-token";
        String email = "ghostUser";

        when(jwtUtil.extractEmail(token)).thenReturn(email);
        when(userRepository.findByEmailAndActiveTrue(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.deleteAccount(token))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found.");

        verify(userRepository, never()).delete(any());
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenTokenIsValid() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        UserEntity userEntity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .active(true)
                .build();

        when(tokenService.validateToken("valid-refresh-token", TokenType.REFRESH)).thenReturn(userEntity);
        when(userMapper.toDetails(any(UserEntity.class))).thenAnswer(i -> {
            UserEntity entity = i.getArgument(0);
            CustomUserDetails mockDetails = new CustomUserDetails();
            mockDetails.setUsername(entity.getUsername());
            mockDetails.setEmail(entity.getEmail());
            mockDetails.setRole(entity.getRole());
            return mockDetails;
        });
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn("new-jwt-token");
        when(tokenService.createRefreshToken(any(UserEntity.class))).thenReturn("new-refresh-token");

        AuthenticationResponse response = authenticationService.refreshToken(request);

        assertThat(response.getToken()).isEqualTo("new-jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(tokenService).markTokenAsUsed("valid-refresh-token", TokenType.REFRESH);
    }

    @Test
    void logout_ShouldRevokeRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        authenticationService.logout(request);

        verify(tokenService).markTokenAsUsed("valid-refresh-token", TokenType.REFRESH);
    }
}