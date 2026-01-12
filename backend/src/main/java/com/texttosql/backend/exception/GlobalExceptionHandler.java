package com.texttosql.backend.exception;

import com.texttosql.backend.dto.error.ErrorResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicatedResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatedResourceException(DuplicatedResourceException e) {
        log.error("DuplicatedResourceException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.error("ResourceNotFoundException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.error("UsernameNotFoundException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException e) {
        log.error("BadCredentialsException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(ExpiredJwtException e) {
        log.error("ExpiredJwtException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Token has expired");
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(SignatureException e) {
        log.error("SignatureException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid token signature");
    }

    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(MalformedJwtException e) {
        log.error("MalformedJwtException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Malformed token");
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JwtException e) {
        log.error("JwtException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid token");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = String.format("Method '%s' not supported. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        log.error("Method not supported: {}", message);

        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.error("Malformed JSON request: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request or missing request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String requiredType = Objects.nonNull(ex.getRequiredType()) ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("The parameter '%s' with value '%s' could not be converted to type '%s'",
                ex.getName(), ex.getValue(), requiredType);

        log.error("Type mismatch: {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Validation error: {}", errorMessage);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed: " + errorMessage);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("No resource found: {} {}", e.getHttpMethod(), e.getResourcePath());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested endpoint or resource does not exist.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("Database integrity violation: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Database error: Invalid data format or constraint violation.");
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException e) {
        log.error("LLM Service Unreachable: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "LLM Service unavailable: " + e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access denied: You do not have permission to perform this action. " + e.getMessage());
    }

    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorResponse> handleServletException(ServletException e) {
        log.warn("Error accessing Servlet: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Error accessing Servlet: " + e.getMessage());
    }

    @ExceptionHandler(SchemaImportException.class)
    public ResponseEntity<ErrorResponse> handleSchemaImportException(SchemaImportException e) {

        log.error("Schema import failed: {}", e.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Schema import failed: " + e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}