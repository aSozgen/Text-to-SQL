package com.texttosql.backend.controller;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.dto.SchemaImportRequest;
import com.texttosql.backend.dto.TableDto;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.SchemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schemas")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaService schemaService;

    @PostMapping("/import")
    public ResponseEntity<DatabaseDto> importDatabase(@Valid @RequestBody SchemaImportRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.importDatabase(request, userDetails));
    }

    @GetMapping("/databases")
    public ResponseEntity<Page<DatabaseDto>> getDatabases(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(schemaService.getDatabases(userDetails, page, size, sort, direction));
    }

    @GetMapping("/databases/{databaseId}")
    public ResponseEntity<DatabaseDto> getDatabase(@PathVariable UUID databaseId,
                                                   @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(schemaService.getDatabase(databaseId, userDetails));
    }

    @PostMapping("/databases")
    public ResponseEntity<DatabaseDto> createDatabase(@Valid @RequestBody DatabaseDto databaseDTO,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.createDatabase(databaseDTO, userDetails));
    }

    @PatchMapping("/databases/{databaseId}")
    public ResponseEntity<DatabaseDto> updateDatabase(@PathVariable UUID databaseId,
                                                      @Valid @RequestBody DatabaseDto databaseDTO,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(schemaService.updateDatabase(databaseId, databaseDTO, userDetails));
    }

    @DeleteMapping("/databases/{databaseId}")
    public ResponseEntity<Void> deleteDatabase(@PathVariable UUID databaseId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        schemaService.deleteDatabase(databaseId, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/databases/{databaseId}/tables")
    public ResponseEntity<Page<TableDto>> getTables(@PathVariable UUID databaseId,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(defaultValue = "createdAt") String sort,
                                                    @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(schemaService.getTables(databaseId, userDetails, page, size, sort, direction));
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<TableDto> getTable(@PathVariable UUID databaseId,
                                             @PathVariable UUID tableId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(schemaService.getTable(databaseId, tableId, userDetails));
    }

    @PostMapping("/databases/{databaseId}/tables")
    public ResponseEntity<TableDto> createTable(@PathVariable UUID databaseId,
                                                @Valid @RequestBody TableDto tableDTO,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(schemaService.createTable(databaseId, tableDTO, userDetails));
    }

    @PatchMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<TableDto> updateTable(@PathVariable UUID databaseId,
                                                @PathVariable UUID tableId,
                                                @Valid @RequestBody TableDto tableDTO,
                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(schemaService.updateTable(databaseId, tableId, tableDTO, userDetails));
    }

    @DeleteMapping("/databases/{databaseId}/tables/{tableId}")
    public ResponseEntity<Void> deleteTable(@PathVariable UUID databaseId,
                                            @PathVariable UUID tableId,
                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        schemaService.deleteTable(databaseId, tableId, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}/columns")
    public ResponseEntity<Page<ColumnDto>> getColumns(@PathVariable UUID databaseId,
                                                      @PathVariable UUID tableId,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size,
                                                      @RequestParam(defaultValue = "createdAt") String sort,
                                                      @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(schemaService.getColumns(databaseId, tableId, userDetails, page, size, sort, direction));
    }

    @GetMapping("/databases/{databaseId}/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<ColumnDto> getColumn(@PathVariable UUID databaseId,
                                               @PathVariable UUID tableId,
                                               @PathVariable UUID columnId,
                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(schemaService.getColumn(databaseId, tableId, columnId, userDetails));
    }

    @PostMapping("/databases/{databaseId}/tables/{tableId}/columns")
    public ResponseEntity<ColumnDto> createColumn(@PathVariable UUID databaseId,
                                                  @PathVariable UUID tableId,
                                                  @Valid @RequestBody ColumnDto columnDto,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemaService.createColumn(databaseId, tableId, columnDto, userDetails));
    }

    @PatchMapping("/databases/{databaseId}/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<ColumnDto> updateColumn(@PathVariable UUID databaseId,
                                                  @PathVariable UUID tableId,
                                                  @PathVariable UUID columnId,
                                                  @Valid @RequestBody ColumnDto columnDto,
                                                  @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(schemaService.updateColumn(databaseId, tableId, columnId, columnDto, userDetails));
    }

    @DeleteMapping("/databases/{databaseId}/tables/{tableId}/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@PathVariable UUID databaseId,
                                             @PathVariable UUID tableId,
                                             @PathVariable UUID columnId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        schemaService.deleteColumn(databaseId, tableId, columnId, userDetails);
        return ResponseEntity.noContent().build();
    }
}