package com.texttosql.backend.service;

import com.texttosql.backend.dto.DatabaseDTO;
import com.texttosql.backend.dto.TableDTO;
import com.texttosql.backend.entity.DatabaseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final DatabaseService databaseService;
    private final TableService tableService;

    public List<DatabaseDTO> getDatabases() {
        return databaseService.getDatabases();
    }

    public DatabaseDTO createDatabase(@Valid DatabaseDTO databaseDTO) {
        return databaseService.createDatabase(databaseDTO);
    }

    public DatabaseDTO getDatabase(UUID  databaseId) {
        return databaseService.getDatabase(databaseId);
    }

    public DatabaseDTO updateDatabase(UUID  databaseId, @Valid DatabaseDTO databaseDTO) {
        return databaseService.updateDatabase(databaseId, databaseDTO);
    }

    public void deleteDatabase(UUID  databaseId) {
        databaseService.deleteDatabase(databaseId);
    }

    private DatabaseEntity getCurrentDatabaseEntity(UUID databaseId) {
        return databaseService.getDatabaseEntity(databaseId);
    }

    public List<TableDTO> getTables(UUID databaseId) {
        return tableService.getTables(getCurrentDatabaseEntity(databaseId));
    }

    public TableDTO getTable(UUID tableId) {
        return tableService.getTable(tableId);
    }

    public TableDTO createTable(UUID databaseId, @Valid TableDTO tableDTO) {
        return tableService.createTable(getCurrentDatabaseEntity(databaseId), tableDTO);
    }

    public TableDTO updateTable(UUID databaseId, UUID tableId, @Valid TableDTO tableDTO) {
        return tableService.updateTable(getCurrentDatabaseEntity(databaseId), tableId, tableDTO);
    }

    public void deleteTable(UUID databaseId, UUID tableId) {
        tableService.deleteTable(tableId);
    }
}
