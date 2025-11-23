package com.texttosql.backend.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConversationTurn(
        @JsonProperty("question")
        String question,

        @JsonProperty("sql")
        String sql
) {
}