package com.texttosql.backend.controller;

import com.texttosql.backend.dto.auth.AuthResponse;
import com.texttosql.backend.dto.auth.ErrorResponse;
import com.texttosql.backend.dto.auth.LoginRequest;
import com.texttosql.backend.dto.auth.RegisterRequest;
import com.texttosql.backend.dto.auth.UserInfoResponse;
import com.texttosql.backend.exception.AuthenticationException;
import com.texttosql.backend.exception.BadRequestException;
import com.texttosql.backend.exception.DuplicateResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.exception.UnauthorizedException;
import com.texttosql.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        log.info("Register request received for username: {} from IP: {}",
                request.username(), getClientIp(httpRequest));

        AuthResponse response = authService.register(request);

        log.info("User registered successfully: {}", request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        log.info("Login request received for: {} from IP: {}",
                request.email(), getClientIp(httpRequest));

        AuthResponse response = authService.login(request);

        log.info("User logged in successfully: {}", request.email());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getCurrentUser() {
        log.debug("Get current user info request");

        UserInfoResponse response = authService.getCurrentUser();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "Auth Service");
        health.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(health);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Invalid input data",
                LocalDateTime.now(),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        log.error("Duplicate resource: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Duplicate Resource",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.error("Authentication failed: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication Failed",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request) {

        log.error("Unauthorized access: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.error("Resource not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {

        log.error("Bad request: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error: ", ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}