package com.texttosql.backend.service;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.dto.TableDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final DatabaseService databaseService;
    private final TableService tableService;
    private final ColumnService columnService;
    private final MessageService messageService;
    private final UserMapper userMapper;

    private boolean isVersionUsedInMessages(UUID databaseId, CustomUserDetails userDetails) {
        return messageService.isVersionUsedInMessages(databaseId, getCurrentDatabaseEntity(databaseId, userDetails).getCurrentVersion());
    }

    public Page<DatabaseDto> getDatabases(CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        return databaseService.getDatabases(userDetails, page, size, sort, direction);
    }

    public DatabaseDto createDatabase(DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        return databaseService.createDatabase(databaseDTO, userDetails);
    }

    public DatabaseDto getDatabase(UUID databaseId, CustomUserDetails userDetails) {
        return databaseService.getDatabase(databaseId, userDetails);
    }

    public DatabaseDto updateDatabase(UUID databaseId, DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        return databaseService.updateDatabase(databaseId, databaseDTO, userDetails, isVersionUsedInMessages(databaseId, userDetails));
    }

    public void deleteDatabase(UUID databaseId, CustomUserDetails userDetails) {
        databaseService.deleteDatabase(databaseId ,userDetails);
    }

    private DatabaseEntity getCurrentDatabaseEntity(UUID databaseId, CustomUserDetails userDetails) {
        return databaseService.getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));
    }

    public Page<TableDto> getTables(UUID databaseId, CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        return tableService.getTables(getCurrentDatabaseEntity(databaseId, userDetails), page, size, sort, direction);
    }

    public TableDto getTable(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return tableService.getTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId);
    }

    public TableDto createTable(UUID databaseId, TableDto tableDTO, CustomUserDetails userDetails) {
        return tableService.createTable(getCurrentDatabaseEntity(databaseId, userDetails), tableDTO, isVersionUsedInMessages(databaseId, userDetails));
    }

    public TableDto updateTable(UUID databaseId, UUID tableId, TableDto tableDTO, CustomUserDetails userDetails) {
        return tableService.updateTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId, tableDTO, isVersionUsedInMessages(databaseId, userDetails));
    }

    public void deleteTable(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        tableService.deleteTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId, isVersionUsedInMessages(databaseId, userDetails));
    }

    private TableEntity getCurrentTableEntity(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return tableService.getCurrentTableEntity(getCurrentDatabaseEntity(databaseId, userDetails), tableId);
    }

    public Page<ColumnDto> getColumns(UUID databaseId, UUID tableId, CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        return columnService.getColumns(getCurrentTableEntity(databaseId, tableId, userDetails), page, size, sort, direction);
    }

    public ColumnDto getColumn(UUID databaseId, UUID tableId, UUID columnId, CustomUserDetails userDetails) {
        return columnService.getColumn(getCurrentTableEntity(databaseId, tableId, userDetails) ,columnId);
    }

    public ColumnDto createColumn(UUID databaseId, UUID tableId, ColumnDto columnDto, CustomUserDetails userDetails) {
        return columnService.createColumn(getCurrentDatabaseEntity(databaseId, userDetails), getCurrentTableEntity(databaseId, tableId, userDetails), columnDto, isVersionUsedInMessages(databaseId, userDetails));
    }

    public ColumnDto updateColumn(UUID databaseId, UUID tableId, UUID columnId, ColumnDto columnDto, CustomUserDetails userDetails) {
        return columnService.updateColumn(getCurrentDatabaseEntity(databaseId, userDetails), getCurrentTableEntity(databaseId, tableId, userDetails), columnId, columnDto, isVersionUsedInMessages(databaseId, userDetails));
    }

    public void deleteColumn(UUID databaseId, UUID tableId, UUID columnId, CustomUserDetails userDetails) {
        columnService.deleteColumn(getCurrentDatabaseEntity(databaseId, userDetails), getCurrentTableEntity(databaseId, tableId, userDetails), columnId, isVersionUsedInMessages(databaseId, userDetails));
    }
}
