package com.texttosql.backend.dto;

import com.texttosql.backend.entity.enums.Feedback;

public record FeedbackRequest(Feedback feedback) {
}
