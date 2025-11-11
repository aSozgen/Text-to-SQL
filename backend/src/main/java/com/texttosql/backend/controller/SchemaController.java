package com.texttosql.backend.controller;

import com.texttosql.backend.dto.DatabaseDTO;
import com.texttosql.backend.dto.TableDTO;
import com.texttosql.backend.service.SchemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schemas")
@RequiredArgsConstructor
class SchemaController {

    private final SchemaService schemaService;

    @GetMapping("/databases")
    private ResponseEntity<List<DatabaseDTO>> getDatabases() {
        return ResponseEntity.ok(schemaService.getDatabases());
    }

    @GetMapping("/databases/{databaseId}")
    private ResponseEntity<DatabaseDTO> getDatabase(@PathVariable UUID databaseId) {
        return ResponseEntity.ok(schemaService.getDatabase(databaseId));
    }

    @PostMapping("/databases")
    private ResponseEntity<DatabaseDTO> createDatabase(@Valid @RequestBody DatabaseDTO databaseDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaService.createDatabase(databaseDTO));
    }

    @PatchMapping("/databases/{databaseId}")
    private ResponseEntity<DatabaseDTO> updateDatabase(@PathVariable UUID databaseId, @Valid @RequestBody DatabaseDTO databaseDTO) {
        return ResponseEntity.ok(schemaService.updateDatabase(databaseId, databaseDTO));
    }

    @DeleteMapping("/databases/{databaseId}")
    private ResponseEntity<Void> deleteDatabase(@PathVariable UUID databaseId) {
        schemaService.deleteDatabase(databaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/databases/{databaseId}/tables")
    public ResponseEntity<List<TableDTO>> getTables(@PathVariable UUID databaseId) {
        return ResponseEntity.ok(schemaService.getTables(databaseId));
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<TableDTO> getTable(@PathVariable UUID tableId, @PathVariable UUID databaseId) {
        return ResponseEntity.ok(schemaService.getTable(tableId));
    }

    @PostMapping("/databases/{databaseId}/tables")
    public ResponseEntity<TableDTO> createTable(@PathVariable UUID databaseId,
                                                @Valid @RequestBody TableDTO tableDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.createTable(databaseId, tableDTO));
    }

    @PatchMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<TableDTO> updateTable(@PathVariable UUID databaseId,
                                                @PathVariable UUID tableId,
                                                @Valid @RequestBody TableDTO tableDTO) {
        return ResponseEntity.ok(schemaService.updateTable(databaseId, tableId, tableDTO));
    }

    @DeleteMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID databaseId, @PathVariable UUID tableId) {
        schemaService.deleteTable(databaseId, tableId);
        return ResponseEntity.noContent().build();
    }
}
