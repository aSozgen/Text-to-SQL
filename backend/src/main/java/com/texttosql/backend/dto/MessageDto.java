package com.texttosql.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.texttosql.backend.util.Feedback;
import com.texttosql.backend.util.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {

    UUID messageId;
    UUID databaseId;

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
    String content;
    Double confidence;
    SenderType senderType;
    Feedback feedback;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDateTime createdAt;
}