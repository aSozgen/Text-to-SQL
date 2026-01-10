package com.texttosql.backend.client;

import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public LLMResponse generateSql(LLMRequest request) {
        log.info("Sending request to LLM Service: {}", request);

        return restClient.post()
                .uri(predictionEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    log.error("LLM Service Client Error: {} {}", resp.getStatusCode(), resp.getStatusText());
                    throw new IllegalArgumentException("LLM Service Client Error: " + resp.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    log.error("LLM Service Server Error: {} {}", resp.getStatusCode(), resp.getStatusText());
                    throw new RuntimeException("LLM Service External Server Error: " + resp.getStatusCode());
                })
                .body(LLMResponse.class);
    }
}