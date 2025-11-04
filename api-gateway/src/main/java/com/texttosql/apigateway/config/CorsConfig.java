package com.texttosql.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${gateway.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(allowedOrigins);
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-User-Id",
                "X-Username"
        ));
        corsConfig.setExposedHeaders(Arrays.asList(
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Retry-After",
                "X-User-Id",
                "X-Username"
        ));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
