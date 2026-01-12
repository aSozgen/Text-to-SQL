package com.texttosql.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDto {

    UUID tableId;

    @NotBlank(message = "Table name cannot be empty")
    @Size(min = 2, max = 64, message = "Table name must be between 2 and 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]*$", message = "Name contains invalid characters")
    String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    String description;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDateTime createdAt;
}