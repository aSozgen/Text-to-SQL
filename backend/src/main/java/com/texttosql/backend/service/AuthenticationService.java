package com.texttosql.backend.service;

import com.texttosql.backend.dto.auth.AuthenticationResponse;
import com.texttosql.backend.dto.auth.LoginRequest;
import com.texttosql.backend.dto.auth.RegisterRequest;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.util.JwtUtil;
import com.texttosql.backend.util.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public AuthenticationResponse register(RegisterRequest registerRequest) {

        String username = registerRequest.username();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicatedResourceException("A user with the username '" + username + "' already exists");
        }

        String email = registerRequest.email();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicatedResourceException("A user with the email '" + email + "' already exists");
        }

        UserEntity newUser = UserEntity.builder()
                .username(registerRequest.username())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(Role.USER)
                .active(true)
                .build();

        UserEntity savedUser = userRepository.save(newUser);
        String jwtToken = jwtUtil.generateToken(userMapper.toDto(savedUser));

        return new AuthenticationResponse(jwtToken);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        UserEntity user = userRepository.findByUsernameAndActiveTrue(loginRequest.username()).
                orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + loginRequest.username()));
        String jwtToken = jwtUtil.generateToken(userMapper.toDto(user));

        return new AuthenticationResponse(jwtToken);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse validateToken(String token) {

        if (!jwtUtil.validateToken(token)) {
            throw new BadCredentialsException("Invalid or expired token");
        }

        return new AuthenticationResponse(token);
    }
}
