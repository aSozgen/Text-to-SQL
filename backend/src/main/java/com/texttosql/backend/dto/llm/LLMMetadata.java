package com.texttosql.backend.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LLMMetadata(
        @JsonProperty("model_version") String modelVersion,
        String timestamp,
        String device,
        @JsonProperty("num_beams") Integer numBeams,
        @JsonProperty("input_length") Integer inputLength,
        @JsonProperty("max_context_turns") Integer maxContextTurns
) {}

