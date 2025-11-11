package com.texttosql.backend.service;

import com.texttosql.backend.dto.auth.AuthenticationResponse;
import com.texttosql.backend.dto.auth.LoginRequest;
import com.texttosql.backend.dto.auth.RegisterRequest;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.util.JwtUtil;
import com.texttosql.backend.util.RoleEnum;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticationResponse register(@Valid RegisterRequest registerRequest) {

        String username = registerRequest.username();
        if(userRepository.existsByUsername(username)) {
            throw new DuplicatedResourceException("A user with the username '" + username + "' already exists");
        }

        String email = registerRequest.email();
        if(userRepository.existsByEmail(email)) {
            throw new DuplicatedResourceException("A user with the email '" + email + "' already exists");
        }

        UserEntity newUser = UserEntity.builder()
                .username(registerRequest.username())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(RoleEnum.USER)
                .active(true)
                .build();

        UserEntity savedUser = userRepository.save(newUser);
        String jwtToken = jwtUtil.generateToken(CustomUserDetails.fromUserEntity(savedUser));

        return new AuthenticationResponse(jwtToken);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse login(@Valid LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        UserEntity user = userRepository.findByUsernameAndActiveTrue(loginRequest.username()).
                orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + loginRequest.username()));
        String jwtToken = jwtUtil.generateToken(CustomUserDetails.fromUserEntity(user));

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
