package com.texttosql.backend.client;

import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

@Component
@Slf4j
public class LlmClient {

    private final RestClient restClient;
    private final String predictionEndpoint;

    public LlmClient(@Value("${llm.service.url}") String baseUrl,
                     @Value("${llm.service.endpoint}") String endpoint,
                     @Value("${llm.service.read-timeout}") int readTimeoutMs,
                     @Value("${llm.service.connection-timeout}") int connectionTimeoutMs) {

        this.predictionEndpoint = endpoint;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Cacheable(value = "llmResponses", key = "#request.question + '_' + (#request.schema != null ? #request.schema.hashCode() : 'null')", unless = "#result == null")
    public LLMResponse generateSql(LLMRequest request) {
        Instant start = Instant.now();
        log.info("Sending request to LLM Service - Question length: {} chars", request.question().length());

        try {
            LLMResponse response = restClient.post()
                    .uri(predictionEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        log.error("LLM Service Client Error: {} {}", resp.getStatusCode(), resp.getStatusText());
                        throw new IllegalArgumentException("Invalid request to LLM service: " + resp.getStatusText());
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        log.error("LLM Service Server Error: {} {}", resp.getStatusCode(), resp.getStatusText());
                        throw new RuntimeException("LLM service is temporarily unavailable. Please try again.");
                    })
                    .body(LLMResponse.class);

            Duration duration = Duration.between(start, Instant.now());
            log.info("LLM Response received in {} ms - Confidence: {}",
                    duration.toMillis(), response != null ? response.confidence() : "N/A");

            return response;

        } catch (ResourceAccessException e) {
            log.error("LLM Service timeout or connection error", e);
            throw new RuntimeException("LLM service is not responding. Please try again later.", e);
        } catch (Exception e) {
            log.error("Unexpected error calling LLM service", e);
            throw new RuntimeException("Failed to generate SQL query. Please try again.", e);
        }
    }
}