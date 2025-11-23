package com.texttosql.backend.dto;

import com.texttosql.backend.util.FeedbackEnum;
import com.texttosql.backend.util.SenderTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    UUID messageId;

    @NotBlank(message = "Message content is required")
    @Size(max = 2000, message = "Message content cannot exceed 2000 characters")
    String content;
    Map<String, Object> schema;
    SenderTypeEnum senderType;
    FeedbackEnum feedback;
}