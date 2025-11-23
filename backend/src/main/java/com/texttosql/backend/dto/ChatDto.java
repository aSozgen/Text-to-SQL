package com.texttosql.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDto {

    UUID chatId;

    @NotBlank(message = "Chat name is required")
    @Size(min = 1, max = 50, message = "Chat name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_ ]*$", message = "Name contains invalid characters")
    String name;

}