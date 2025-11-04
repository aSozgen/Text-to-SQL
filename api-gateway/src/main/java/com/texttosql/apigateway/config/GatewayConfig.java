package com.texttosql.apigateway.config;

import com.texttosql.apigateway.filter.GlobalErrorFilter;
import com.texttosql.apigateway.filter.JwtAuthenticationFilter;
import com.texttosql.apigateway.filter.LoggingFilter;
import com.texttosql.apigateway.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final LoggingFilter loggingFilter;
    private final GlobalErrorFilter globalErrorFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("health_route", r -> r
                        .path("/health", "/actuator/health")
                        .filters(f -> f.filter(loggingFilter))
                        .uri("http://localhost:8080"))

                .route("auth_routes", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f
                                .filter(globalErrorFilter)
                                .filter(loggingFilter)
                                .filter(rateLimitFilter.apply(
                                        new RateLimitFilter.Config(10, 20)))
                                .rewritePath("/api/auth/(?<segment>.*)", "/api/auth/${segment}")
                                .circuitBreaker(config -> config
                                        .setName("authCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth")))
                        .uri("http://localhost:8080"))

                .route("protected_routes", r -> r
                        .path("/api/**")
                        .filters(f -> f
                                .filter(globalErrorFilter)
                                .filter(loggingFilter)
                                .filter(jwtAuthenticationFilter.apply(
                                        new JwtAuthenticationFilter.Config()))
                                .filter(rateLimitFilter.apply(
                                        new RateLimitFilter.Config(100, 200)))
                                .rewritePath("/api/(?<segment>.*)", "/api/${segment}")
                                .circuitBreaker(config -> config
                                        .setName("backendCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/service")))
                        .uri("http://localhost:8080"))

                .build();
    }
}
