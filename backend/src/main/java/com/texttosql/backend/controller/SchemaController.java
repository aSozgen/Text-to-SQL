package com.texttosql.backend.controller;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.dto.TableDto;
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
    private ResponseEntity<List<DatabaseDto>> getDatabases() {
        return ResponseEntity.ok(schemaService.getDatabases());
    }

    @GetMapping("/databases/{databaseId}")
    private ResponseEntity<DatabaseDto> getDatabase(@PathVariable UUID databaseId) {
        return ResponseEntity.ok(schemaService.getDatabase(databaseId));
    }

    @PostMapping("/databases")
    private ResponseEntity<DatabaseDto> createDatabase(@Valid @RequestBody DatabaseDto databaseDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaService.createDatabase(databaseDTO));
    }

    @PatchMapping("/databases/{databaseId}")
    private ResponseEntity<DatabaseDto> updateDatabase(@PathVariable UUID databaseId, @Valid @RequestBody DatabaseDto databaseDTO) {
        return ResponseEntity.ok(schemaService.updateDatabase(databaseId, databaseDTO));
    }

    @DeleteMapping("/databases/{databaseId}")
    private ResponseEntity<Void> deleteDatabase(@PathVariable UUID databaseId) {
        schemaService.deleteDatabase(databaseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/databases/{databaseId}/tables")
    public ResponseEntity<List<TableDto>> getTables(@PathVariable UUID databaseId) {
        return ResponseEntity.ok(schemaService.getTables(databaseId));
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<TableDto> getTable(@PathVariable UUID databaseId, @PathVariable UUID tableId) {
        return ResponseEntity.ok(schemaService.getTable(databaseId, tableId));
    }

    @PostMapping("/databases/{databaseId}/tables")
    public ResponseEntity<TableDto> createTable(@PathVariable UUID databaseId,
                                                @Valid @RequestBody TableDto tableDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.createTable(databaseId, tableDTO));
    }

    @PatchMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<TableDto> updateTable(@PathVariable UUID databaseId,
                                                @PathVariable UUID tableId,
                                                @Valid @RequestBody TableDto tableDTO) {
        return ResponseEntity.ok(schemaService.updateTable(databaseId, tableId, tableDTO));
    }

    @DeleteMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID databaseId, @PathVariable UUID tableId) {
        schemaService.deleteTable(databaseId, tableId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}/columns")
    public ResponseEntity<List<ColumnDto>> getColumns(@PathVariable UUID tableId) {
        return ResponseEntity.ok(schemaService.getColumns(tableId));
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<ColumnDto> getColumn(@PathVariable UUID columnId) {
        return ResponseEntity.ok(schemaService.getColumn(columnId));
    }

    @PostMapping("/databases/{databaseId}/tables/{tableId}/columns")
    public ResponseEntity<ColumnDto> createColumn(@PathVariable UUID tableId,
                                                @Valid @RequestBody ColumnDto columnDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.createColumn(tableId, columnDto));
    }

    @PatchMapping("/databases/{databaseId}/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<ColumnDto> updateColumn(@PathVariable UUID tableId,
                                                @PathVariable UUID columnId,
                                                @Valid @RequestBody ColumnDto columnDto) {
        return ResponseEntity.ok(schemaService.updateColumn(tableId, columnId, columnDto));
    }

    @DeleteMapping("/databases/{databaseId}/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable UUID columnId) {
        schemaService.deleteColumn(columnId);
        return ResponseEntity.noContent().build();
    }
}
