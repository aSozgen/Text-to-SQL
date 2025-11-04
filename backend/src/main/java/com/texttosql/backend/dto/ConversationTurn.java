package com.texttosql.backend.dto;

public record ConversationTurn(
        String question,
        String sql
) {
}
