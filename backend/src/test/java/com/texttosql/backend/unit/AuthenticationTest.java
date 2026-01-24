package com.texttosql.backend.unit;

import com.texttosql.backend.dto.auth.AuthenticationResponse;
import com.texttosql.backend.dto.auth.LoginRequest;
import com.texttosql.backend.dto.auth.RegisterRequest;
import com.texttosql.backend.dto.entity.UserDto;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.AuthenticationService;
import com.texttosql.backend.util.JwtUtil;
import com.texttosql.backend.util.Role;
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

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_ShouldReturnVoid_WhenRequestIsValid() {
        // Arrange
        RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password");

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");

        authenticationService.register(request);

        verify(userRepository).save(any(UserEntity.class));
        verify(passwordEncoder).encode("password");
    }

    @Test
    void register_ShouldThrowException_WhenUsernameExists() {
        RegisterRequest request = new RegisterRequest("existingUser", "email@test.com", "password");
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("A user with the username 'existingUser' already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest("newUser", "existing@test.com", "password");
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(DuplicatedResourceException.class)
                .hasMessageContaining("A user with the email 'existing@test.com' already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreCorrect() {
        LoginRequest loginRequest = new LoginRequest("testuser", "password");

        UserEntity userEntity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .active(true)
                .build();

        CustomUserDetails userDetailsDto = new CustomUserDetails();
        userDetailsDto.setUserId(userEntity.getUserId());
        userDetailsDto.setUsername("testuser");
        userDetailsDto.setEmail("test@example.com");
        userDetailsDto.setRole(Role.USER);
        userDetailsDto.setActive(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByUsernameAndActiveTrue(loginRequest.username())).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(userDetailsDto);
        when(jwtUtil.generateToken(userDetailsDto)).thenReturn("jwt-token");

        UserDto response = authenticationService.login(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFoundAfterAuth() {
        LoginRequest loginRequest = new LoginRequest("ghost", "password");

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsernameAndActiveTrue("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: ghost");
    }

    @Test
    void login_ShouldPropagateException_WhenAuthenticationFails() {
        LoginRequest loginRequest = new LoginRequest("wrongUser", "wrongPass");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUsernameAndActiveTrue(any());
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
                .hasMessage("Invalid or expired token");
    }
}