package com.texttosql.backend.service;

import com.texttosql.backend.dto.auth.*;
import com.texttosql.backend.dto.entity.UserDto;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.EmailNotVerifiedException;
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
    private final TokenService tokenService;
    private final EmailService emailService;

    @Transactional
    public void register(RegisterRequest registerRequest) {

        String username = registerRequest.username();
        if (userRepository.existsByUsername(username)) {
            throw new DuplicatedResourceException("Username already exists.");
        }

        String email = registerRequest.email();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicatedResourceException("Email already exists.");
        }

        UserEntity newUser = UserEntity.builder()
                .username(registerRequest.username())
                .email(registerRequest.email())
                .password(passwordEncoder.encode(registerRequest.password()))
                .role(Role.USER)
                .active(true)
                .emailVerified(false)
                .build();

        userRepository.save(newUser);

        // Send verification email
        String token = tokenService.createVerificationToken(newUser);
        emailService.sendVerificationEmail(newUser.getEmail(), token);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse login(LoginRequest loginRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        UserEntity user = userRepository.findByEmailAndActiveTrue(loginRequest.email()).
                orElseThrow(() -> new UsernameNotFoundException("User not found."));

        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified. Please check your email for verification link.");
        }

        String jwtToken = jwtUtil.generateToken(userMapper.toDto(user));

        return new AuthenticationResponse(jwtToken);
    }

    @Transactional(readOnly = true)
    public AuthenticationResponse validateToken(String token) {

        if (!jwtUtil.validateToken(token)) {
            throw new BadCredentialsException("Invalid or expired token.");
        }

        return new AuthenticationResponse(token);
    }

    @Transactional(readOnly = true)
    public UserDto getMe(String token) {
        UserEntity user = getUserFromToken(token);

        return new UserDto(user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional
    public void updateProfile(String token, UpdateProfileRequest request) {
        UserEntity user = getUserFromToken(token);

        if (!user.getUsername().equals(request.username()) &&
                userRepository.existsByUsername(request.username())) {
            throw new DuplicatedResourceException("Username already exists.");
        }

        user.setUsername(request.username());
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String token, ChangePasswordRequest request) {
        UserEntity user = getUserFromToken(token);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(String token) {
        UserEntity user = getUserFromToken(token);
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        UserEntity user = tokenService.validateVerificationToken(token);
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenService.markVerificationTokenAsUsed(token);
    }

    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getEmailVerified()) {
            throw new DuplicatedResourceException("Email is already verified");
        }

        String token = tokenService.createVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = tokenService.createPasswordResetToken(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = tokenService.validatePasswordResetToken(request.token());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        tokenService.markPasswordResetTokenAsUsed(request.token());
    }

    private UserEntity getUserFromToken(String token) {
        String email = jwtUtil.extractUsername(token);

        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
