package com.texttosql.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMResponse(
        String status,
        String sql,
        @JsonProperty("is_valid") Boolean isValid,
        @JsonProperty("validation_error") String validationError,
        Double confidence,
        @JsonProperty("schema_used") Boolean schemaUsed,
        @JsonProperty("context_used") Boolean contextUsed,
        @JsonProperty("context_turns") Integer contextTurns,
        @JsonProperty("processing_time_ms") Long processingTimeMs,
        String error,
        LLMMetadata metadata
) {
}
