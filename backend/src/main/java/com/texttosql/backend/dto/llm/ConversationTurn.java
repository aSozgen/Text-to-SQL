package com.texttosql.backend.dto.llm;

public record ConversationTurn(
        String question,
        String sql
) {
}
