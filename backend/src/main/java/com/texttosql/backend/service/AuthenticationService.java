package com.texttosql.backend.service;

import com.texttosql.backend.dto.auth.*;
import com.texttosql.backend.dto.entity.UserDto;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.entity.enums.TokenType;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.EmailNotVerifiedException;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.UserRepository;
import com.texttosql.backend.util.JwtUtil;
import com.texttosql.backend.entity.enums.Role;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

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
    private final SchemaService schemaService;
    @Value("${guest.email.domain}")
    private String guestEmailDomain;
    @Value( "${token.jwt.refresh.expiration}")
    private Long refreshTokenExpiryTime;

    @Transactional
    public AuthenticationResponse generateGuestToken(HttpServletResponse response) {
        String uuid = UUID.randomUUID().toString();
        String prefix = uuid.substring(0, 8);
        String guestEmail = "guest_" + prefix + guestEmailDomain;
        String randomPassword = UUID.randomUUID().toString();

        UserEntity guestUser = UserEntity.builder()
                .username("Guest")
                .email(guestEmail)
                .password(passwordEncoder.encode(randomPassword))
                .role(Role.GUEST)
                .active(true)
                .emailVerified(true)
                .build();

        userRepository.save(guestUser);

        String jwtToken = jwtUtil.generateToken(userMapper.toDetails(guestUser));
        String refreshToken = tokenService.createRefreshToken(guestUser);
        
        setRefreshTokenCookie(response, refreshToken);
        return new AuthenticationResponse(jwtToken);
    }

    @Transactional
    public void register(RegisterRequest registerRequest, String guestToken) {
        String email = registerRequest.email();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicatedResourceException("Email already exists.");
        }

        UserEntity user;

        if (guestToken != null && jwtUtil.validateToken(guestToken)) {
            String guestEmail = jwtUtil.extractEmail(guestToken);
            user = userRepository.findByEmailAndActiveTrue(guestEmail)
                    .orElseThrow(() -> new EmailNotVerifiedException("Guest user not found."));

            if (user.getRole() != Role.GUEST) {
                 throw new IllegalArgumentException("Token does not belong to a guest user.");
            }

            user.setUsername(registerRequest.username());
            user.setEmail(registerRequest.email());
            user.setPassword(passwordEncoder.encode(registerRequest.password()));
            user.setRole(Role.USER);
            user.setEmailVerified(false);
        } else {
            user = UserEntity.builder()
                    .username(registerRequest.username())
                    .email(registerRequest.email())
                    .password(passwordEncoder.encode(registerRequest.password()))
                    .role(Role.USER)
                    .active(true)
                    .emailVerified(false)
                    .build();
        }

        userRepository.save(user);

        // Send verification email
        String token = tokenService.createEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public AuthenticationResponse login(LoginRequest loginRequest, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        UserEntity user = userRepository.findByEmailAndActiveTrue(loginRequest.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));


        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException("Email not verified. Please check your email for verification link.");
        }

        String jwtToken = jwtUtil.generateToken(userMapper.toDetails(user));
        String refreshToken = tokenService.createRefreshToken(user);

        setRefreshTokenCookie(response, refreshToken);
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
        UserEntity user = tokenService.validateToken(token, TokenType.VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
        tokenService.markTokenAsUsed(token, TokenType.VERIFICATION);

        // Copy templates
        schemaService.copyTemplatesToUser(userMapper.toDetails(user));
    }

    @Transactional
    public void resendVerificationEmail(EmailRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getEmailVerified()) {
            throw new DuplicatedResourceException("Email is already verified");
        }

        String token = tokenService.createEmailVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    @Transactional
    public void forgotPassword(EmailRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = tokenService.createPasswordResetToken(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = tokenService.validateToken(request.token(), TokenType.PASSWORD);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        tokenService.markTokenAsUsed(request.token(), TokenType.PASSWORD);
    }

    @Transactional
    public AuthenticationResponse refreshToken(String refreshToken, HttpServletResponse response) {
        UserEntity user = tokenService.validateToken(refreshToken, TokenType.REFRESH);

        // Revoke the old refresh token
        tokenService.markTokenAsUsed(refreshToken, TokenType.REFRESH);

        // Generate new tokens
        String jwtToken = jwtUtil.generateToken(userMapper.toDetails(user));
        String newRefreshToken = tokenService.createRefreshToken(user);

        setRefreshTokenCookie(response, newRefreshToken);
        return new AuthenticationResponse(jwtToken);
    }

    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null) {
            tokenService.markTokenAsUsed(refreshToken, TokenType.REFRESH);
        }
        deleteRefreshTokenCookie(response);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Should be true in production (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(refreshTokenExpiryTime.intValue() / 1000);
        response.addCookie(cookie);
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private UserEntity getUserFromToken(String token) {
        String email = jwtUtil.extractEmail(token);

        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
