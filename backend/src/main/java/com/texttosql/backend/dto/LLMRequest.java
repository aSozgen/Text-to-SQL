package com.texttosql.backend.dto;

import java.util.List;

public record LLMRequest(
        String question,
        String schema,
        List<ConversationTurn> conversationHistory
) {
    public LLMRequest(String question) {
        this(question, null, null);
    }

    public LLMRequest(String question, String schema) {
        this(question, schema, null);
    }

    public LLMRequest(String question, List<ConversationTurn> conversationHistory) {
        this(question, null, conversationHistory);
    }
}
