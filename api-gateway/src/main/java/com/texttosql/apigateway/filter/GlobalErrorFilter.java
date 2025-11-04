package com.texttosql.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class GlobalErrorFilter implements GatewayFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(throwable -> {
                    log.error("Error processing request: {} {}",
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath(),
                            throwable);

                    return handleError(exchange, throwable);
                });
    }

    private Mono<Void> handleError(ServerWebExchange exchange, Throwable throwable) {
        HttpStatus status = determineHttpStatus(throwable);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", throwable.getMessage());
        errorResponse.put("status", status.value());
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", exchange.getRequest().getPath().toString());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating error response", e);
            byte[] bytes = "{\"error\":\"Internal server error\"}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }

    private HttpStatus determineHttpStatus(Throwable throwable) {
        String message = throwable.getMessage();

        if (message != null) {
            if (message.contains("Connection refused") || message.contains("Connection timed out")) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (message.contains("timeout")) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}