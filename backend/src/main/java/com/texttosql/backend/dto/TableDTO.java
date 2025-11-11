package com.texttosql.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableDTO {
    @NotBlank
    UUID tableId;
    @NotBlank
    @Size(min = 3, max = 20)
    String name;
    @Size(max = 255)
    String description;
}
