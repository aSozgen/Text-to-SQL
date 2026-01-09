package com.texttosql.backend.service;

import com.texttosql.backend.dto.ColumnDto;
import com.texttosql.backend.dto.DatabaseDto;
import com.texttosql.backend.dto.TableDto;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.security.CustomUserDetails;
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
    private final UserMapper userMapper;

    public List<DatabaseDto> getDatabases(CustomUserDetails userDetails) {
        return databaseService.getDatabases(userDetails);
    }

    public DatabaseDto createDatabase(@Valid DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        return databaseService.createDatabase(databaseDTO, userDetails);
    }

    public DatabaseDto getDatabase(UUID databaseId, CustomUserDetails userDetails) {
        return databaseService.getDatabase(databaseId, userDetails);
    }

    public DatabaseDto updateDatabase(UUID databaseId, @Valid DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        return databaseService.updateDatabase(databaseId, databaseDTO ,userDetails);
    }

    public void deleteDatabase(UUID databaseId, CustomUserDetails userDetails) {
        databaseService.deleteDatabase(databaseId ,userDetails);
    }

    private DatabaseEntity getCurrentDatabaseEntity(UUID databaseId, CustomUserDetails userDetails) {
        return databaseService.getCurrentDatabaseEntity(databaseId, userMapper.toEntity(userDetails));
    }

    public List<TableDto> getTables(UUID databaseId, CustomUserDetails userDetails) {
        return tableService.getTables(getCurrentDatabaseEntity(databaseId, userDetails));
    }

    public TableDto getTable(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return tableService.getTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId);
    }

    public TableDto createTable(UUID databaseId, @Valid TableDto tableDTO, CustomUserDetails userDetails) {
        return tableService.createTable(getCurrentDatabaseEntity(databaseId, userDetails), tableDTO);
    }

    public TableDto updateTable(UUID databaseId, UUID tableId, @Valid TableDto tableDTO, CustomUserDetails userDetails) {
        return tableService.updateTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId, tableDTO);
    }

    public void deleteTable(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        tableService.deleteTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId);
    }

    private TableEntity getCurrentTableEntity(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return tableService.getCurrentTableEntity(getCurrentDatabaseEntity(databaseId, userDetails), tableId);
    }

    public List<ColumnDto> getColumns(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return columnService.getColumns(getCurrentTableEntity(databaseId, tableId, userDetails));
    }

    public ColumnDto getColumn(UUID databaseId, UUID tableId, UUID columnId, CustomUserDetails userDetails) {
        return columnService.getColumn(getCurrentTableEntity(databaseId, tableId, userDetails) ,columnId);
    }

    public ColumnDto createColumn(UUID databaseId, UUID tableId, @Valid ColumnDto columnDto, CustomUserDetails userDetails) {
        return columnService.createColumn(getCurrentTableEntity(databaseId, tableId, userDetails), columnDto);
    }

    public ColumnDto updateColumn(UUID databaseId, UUID tableId, UUID columnId, @Valid ColumnDto columnDto, CustomUserDetails userDetails) {
        return columnService.updateColumn(getCurrentTableEntity(databaseId, tableId, userDetails), columnId, columnDto);
    }

    public void deleteColumn(UUID databaseId, UUID tableId, UUID columnId, CustomUserDetails userDetails) {
        columnService.deleteColumn(getCurrentTableEntity(databaseId, tableId, userDetails), columnId);
    }
}
