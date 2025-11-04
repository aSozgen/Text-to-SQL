package com.texttosql.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.texttosql.apigateway.util.RateLimitCache;
import io.github.bucket4j.Bucket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class RateLimitFilter
        extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final RateLimitCache rateLimitCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RateLimitFilter(RateLimitCache rateLimitCache) {
        super(Config.class);
        this.rateLimitCache = rateLimitCache;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = getClientIp(exchange);
            String path = exchange.getRequest().getPath().toString();

            log.debug("Rate limit check for IP: {} on path: {}", clientIp, path);

            Bucket bucket = rateLimitCache.resolveBucket(
                    clientIp,
                    config.getReplenishRate(),
                    config.getBurstCapacity()
            );

            if (bucket.tryConsume(1)) {
                long remainingTokens = bucket.getAvailableTokens();

                exchange.getResponse().getHeaders()
                        .add("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));

                log.debug("Request allowed for IP: {}. Remaining: {}", clientIp, remainingTokens);

                return chain.filter(exchange);
            } else {
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                return onRateLimitExceeded(exchange, clientIp);
            }
        };
    }

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }

    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, String clientIp) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("X-Rate-Limit-Retry-After", "60");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Rate limit exceeded. Please try again later.");
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("retryAfter", 60);
        errorResponse.put("clientIp", clientIp);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error creating rate limit response", e);
            byte[] bytes = "{\"error\":\"Rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private int replenishRate = 100;
        private int burstCapacity = 200;

        public Config(int replenishRate) {
            this.replenishRate = replenishRate;
            this.burstCapacity = replenishRate * 2;
        }
    }
}
