package com.texttosql.backend.unit;

import com.texttosql.backend.dto.auth.*;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.EmailNotVerifiedException;
import com.texttosql.backend.exception.TokenExpiredException;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.AuthenticationService;
import com.texttosql.backend.service.TokenService;
import com.texttosql.backend.service.EmailService;
import com.texttosql.backend.util.JwtUtil;
import com.texttosql.backend.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_ShouldReturnVoid_WhenRequestIsValid() {
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");
        when(tokenService.createVerificationToken(any(UserEntity.class))).thenReturn("token123");

        authenticationService.register(request);

        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).createVerificationToken(any(UserEntity.class));
        verify(emailService).sendVerificationEmail(eq(request.email()), eq("token123"));
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest("newUser", "existing@test.com", "password");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
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

        CustomUserDetails userDto = new CustomUserDetails();
        userDto.setUserId(userId);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setRole(Role.USER);
        userDto.setActive(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmailAndActiveTrue(loginRequest.email())).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDto);
        when(jwtUtil.generateToken(userDto)).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
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

        when(tokenService.validateVerificationToken("valid-token")).thenReturn(userEntity);

        authenticationService.verifyEmail("valid-token");

        verify(tokenService).validateVerificationToken("valid-token");
        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).markVerificationTokenAsUsed("valid-token");
    }

    @Test
    void verifyEmail_ShouldThrowException_WhenTokenIsExpired() {
        when(tokenService.validateVerificationToken("expired-token"))
                .thenThrow(new TokenExpiredException("Verification token has expired"));

        assertThatThrownBy(() -> authenticationService.verifyEmail("expired-token"))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
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

        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
        when(tokenService.createVerificationToken(userEntity)).thenReturn("new-token");

        authenticationService.resendVerificationEmail(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(tokenService).createVerificationToken(userEntity);
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

        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));

        assertThatThrownBy(() -> authenticationService.resendVerificationEmail(request))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("already verified");

        verify(tokenService, never()).createVerificationToken(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void resendVerificationEmail_ShouldThrowException_WhenUserNotFound() {
        ResendVerificationRequest request = new ResendVerificationRequest("nonexistent@example.com");

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

        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(userEntity));
        when(tokenService.createPasswordResetToken(userEntity)).thenReturn("reset-token");

        authenticationService.forgotPassword(request);

        verify(userRepository).findByEmail("test@example.com");
        verify(tokenService).createPasswordResetToken(userEntity);
        verify(emailService).sendPasswordResetEmail("test@example.com", "reset-token");
    }

    @Test
    void forgotPassword_ShouldThrowException_WhenUserNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

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

        when(tokenService.validatePasswordResetToken("valid-reset-token")).thenReturn(userEntity);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        authenticationService.resetPassword(request);

        verify(tokenService).validatePasswordResetToken("valid-reset-token");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(any(UserEntity.class));
        verify(tokenService).markPasswordResetTokenAsUsed("valid-reset-token");
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsExpired() {
        ResetPasswordRequest request = new ResetPasswordRequest("expired-token", "newPassword123");

        when(tokenService.validatePasswordResetToken("expired-token"))
                .thenThrow(new TokenExpiredException("Reset token has expired"));

        assertThatThrownBy(() -> authenticationService.resetPassword(request))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenIsInvalid() {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newPassword123");

        when(tokenService.validatePasswordResetToken("invalid-token"))
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

        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(userRepository.findByEmailAndActiveTrue(email)).thenReturn(Optional.of(userEntity));

        authenticationService.deleteAccount(token);

        verify(userRepository).save(userEntity);
        assertThat(userEntity.getActive()).isFalse();
    }

    @Test
    void deleteAccount_ShouldThrowException_WhenUserNotFound() {
        String token = "valid-token";
        String email = "ghostUser";

        when(jwtUtil.extractUsername(token)).thenReturn(email);
        when(userRepository.findByEmailAndActiveTrue(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.deleteAccount(token))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found.");

        verify(userRepository, never()).delete(any());
    }
}