package com.texttosql.backend.dto.entity;

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
public class ColumnDto {

    UUID columnId;

    UUID tableId;

    UUID databaseId;

    @NotBlank(message = "Column name is required")
    @Size(min = 1, max = 50, message = "Column name must be between 1 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Name contains invalid characters")
    String name;

    @NotBlank(message = "Data type is required")
    @Size(max = 50, message = "Data type usually shouldn't be that long")
    String dataType;
    private boolean isPrimaryKey;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDateTime createdAt;
}