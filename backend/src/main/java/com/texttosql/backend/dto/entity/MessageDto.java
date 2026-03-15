package com.texttosql.backend.dto.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.texttosql.backend.entity.enums.Feedback;
import com.texttosql.backend.entity.enums.SenderType;
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
    UUID chatId;
    UUID databaseId;

    @NotBlank(message = "Message content is required")
    @Size(max = 5000, message = "Message content cannot exceed 5000 characters")
    String content;
    Double confidence;
    SenderType senderType;
    Feedback feedback;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm:ss", timezone = "Europe/Istanbul")
    private LocalDateTime createdAt;
}