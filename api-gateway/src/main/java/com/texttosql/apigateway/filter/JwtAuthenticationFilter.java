package com.texttosql.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttosql.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter
        extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            log.debug("JWT Authentication filter for path: {}", path);

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for path: {}", path);
                return onError(exchange, "Missing Authorization header"
                );
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Invalid Authorization header format");
                return onError(exchange, "Invalid Authorization header format. Use: Bearer <token>"
                );
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.validateToken(token)) {
                    String error = jwtUtil.getValidationError(token);
                    log.warn("Token validation failed: {}", error);
                    return onError(exchange, error != null ? error : "Invalid token"
                    );
                }

                String userId = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);

                log.debug("Authenticated user: {} (ID: {})", username, userId);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", userId)
                        .header("X-Username", username)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                log.error("Token validation error: {}", e.getMessage(), e);
                return onError(exchange, "Token validation failed: " + e.getMessage()
                );
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", exchange.getRequest().getPath().toString());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating error response", e);
            byte[] bytes = "{\"error\":\"Authentication failed\"}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    public static class Config {
        // Empty config class for future extensions
    }
}
