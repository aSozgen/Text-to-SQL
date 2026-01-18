package com.texttosql.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemaImportRequest {
    @NotBlank(message = "Database name cannot be empty")
    @Size(min = 2, max = 64, message = "Database name must be between 2 and 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Name contains invalid characters")
    private String name;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @NotEmpty(message = "JSON content cannot be empty")
    private List<Map<String, Object>> jsonContent;
}