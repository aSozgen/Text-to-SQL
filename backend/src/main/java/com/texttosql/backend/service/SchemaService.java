package com.texttosql.backend.service;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.dto.TableDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
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
    private final ColumnService columnService;

    public List<DatabaseDto> getDatabases() {
        return databaseService.getDatabases();
    }

    public DatabaseDto createDatabase(@Valid DatabaseDto databaseDTO) {
        return databaseService.createDatabase(databaseDTO);
    }

    public DatabaseDto getDatabase(UUID databaseId) {
        return databaseService.getDatabase(databaseId);
    }

    public DatabaseDto updateDatabase(UUID databaseId, @Valid DatabaseDto databaseDTO) {
        return databaseService.updateDatabase(databaseId, databaseDTO);
    }

    public void deleteDatabase(UUID databaseId) {
        databaseService.deleteDatabase(databaseId);
    }

    private DatabaseEntity getCurrentDatabaseEntity(UUID databaseId) {
        return databaseService.getCurrentDatabaseEntity(databaseId);
    }

    public List<TableDto> getTables(UUID databaseId) {
        return tableService.getTables(getCurrentDatabaseEntity(databaseId));
    }

    public TableDto getTable(UUID tableId) {
        return tableService.getTable(tableId);
    }

    public TableDto createTable(UUID databaseId, @Valid TableDto tableDTO) {
        return tableService.createTable(getCurrentDatabaseEntity(databaseId), tableDTO);
    }

    public TableDto updateTable(UUID databaseId, UUID tableId, @Valid TableDto tableDTO) {
        return tableService.updateTable(getCurrentDatabaseEntity(databaseId), tableId, tableDTO);
    }

    public void deleteTable(UUID tableId) {
        tableService.deleteTable(tableId);
    }

    private TableEntity getCurrentTableEntity(UUID tableId) {
        return tableService.getCurrentTableEntity(tableId);
    }

    public List<ColumnDto> getColumns(UUID tableId) {
        return columnService.getColumns(getCurrentTableEntity(tableId));
    }

    public ColumnDto getColumn(UUID columnId) {
        return columnService.getColumn(columnId);
    }

    public ColumnDto createColumn(UUID tableId, @Valid ColumnDto columnDto) {
        return columnService.createColumn(getCurrentTableEntity(tableId), columnDto);
    }

    public ColumnDto updateColumn(UUID tableId, UUID columnId, @Valid ColumnDto columnDto) {
        return columnService.updateColumn(getCurrentTableEntity(tableId), columnId, columnDto);
    }

    public void deleteColumn(UUID columnId) {
        columnService.deleteColumn(columnId);
    }
}
