package com.texttosql.backend.service;

import com.texttosql.backend.dto.SchemaImportRequest;
import com.texttosql.backend.dto.entity.ColumnDto;
import com.texttosql.backend.dto.entity.DatabaseDto;
import com.texttosql.backend.dto.entity.TableDto;
import com.texttosql.backend.dto.search.SchemaSearchResponse;
import com.texttosql.backend.entity.*;
import com.texttosql.backend.exception.SchemaImportException;
import com.texttosql.backend.mapper.DatabaseMapper;
import com.texttosql.backend.mapper.TableMapper;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.ColumnRepository;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.repository.TableRepository;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemaService {

    private final DatabaseService databaseService;
    private final TableService tableService;
    private final ColumnService columnService;
    private final MessageService messageService;
    private final SchemaVersionService versionService;
    private final UserMapper userMapper;
    private final DatabaseMapper databaseMapper;
    private final TableMapper tableMapper;
    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;

    @Transactional
    @CacheEvict(value = "templateSchemas", allEntries = true)
    public DatabaseDto importSchema(SchemaImportRequest request, CustomUserDetails userDetails) {
        try {
            DatabaseDto databaseDto = new DatabaseDto();
            databaseDto.setName(request.getName());
            databaseDto.setDescription(request.getDescription());
            databaseDto.setIsTemplate(request.getIsTemplate());

            DatabaseDto savedDatabase = createDatabase(databaseDto, userDetails);
            UUID databaseId = savedDatabase.getDatabaseId();

            List<Map<String, Object>> rawData = request.getJsonContent();

            Map<String, List<Map<String, Object>>> tablesMap = rawData.stream()
                    .collect(Collectors.groupingBy(row -> String.valueOf(row.get("table_name"))));

            for (Map.Entry<String, List<Map<String, Object>>> entry : tablesMap.entrySet()) {
                String tableName = entry.getKey();
                List<Map<String, Object>> columns = entry.getValue();

                TableDto tableDto = new TableDto();
                tableDto.setName(tableName);
                tableDto.setDescription("Imported via Schema Upload");
                TableDto savedTable = createTable(databaseId, tableDto, userDetails);

                for (Map<String, Object> columnData : columns) {
                    ColumnDto columnDto = new ColumnDto();

                    columnDto.setName(String.valueOf(columnData.get("column_name")));
                    columnDto.setDataType(String.valueOf(columnData.get("data_type")));
                    columnDto.setPrimaryKey(Boolean.parseBoolean(String.valueOf(columnData.get("is_primary_key"))));

                    Object ft = columnData.get("foreign_table");
                    Object fc = columnData.get("foreign_column");
                    columnDto.setForeignTable(ft != null && !ft.equals("null") ? String.valueOf(ft) : null);
                    columnDto.setForeignColumn(fc != null && !fc.equals("null") ? String.valueOf(fc) : null);

                    createColumn(databaseId, savedTable.getTableId(), columnDto, userDetails);
                }
            }

            return savedDatabase;

        } catch (Exception e) {
            throw new SchemaImportException("Schema import failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void copyTemplatesToUser(CustomUserDetails userDetails) {
        List<DatabaseEntity> templates = databaseRepository.findByIsTemplateTrueAndActiveTrue();
        if (templates.isEmpty()) return;

        UserEntity user = userMapper.toEntity(userDetails);

        for (DatabaseEntity template : templates) {
            // 1. Copy Database
            DatabaseEntity newDb = DatabaseEntity.builder()
                    .user(user)
                    .name(template.getName())
                    .description(template.getDescription())
                    .isTemplate(false)
                    .active(true)
                    .currentVersion(0)
                    .build();
            DatabaseEntity savedDb = databaseRepository.save(newDb);

            // 2. Copy Tables
            List<TableEntity> templateTables = tableRepository.findByDatabaseAndActiveTrueOrderByCreatedAtDesc(template);
            for (TableEntity tTable : templateTables) {
                TableEntity newTable = TableEntity.builder()
                        .database(savedDb)
                        .name(tTable.getName())
                        .description(tTable.getDescription())
                        .active(true)
                        .build();
                TableEntity savedTable = tableRepository.save(newTable);

                // 3. Copy Columns
                List<ColumnEntity> templateColumns = columnRepository.findByTableAndActiveTrueOrderByCreatedAtDesc(tTable);
                List<ColumnEntity> newColumns = templateColumns.stream()
                        .map(tc -> ColumnEntity.builder()
                                .table(savedTable)
                                .name(tc.getName())
                                .dataType(tc.getDataType())
                                .isPrimaryKey(tc.isPrimaryKey())
                                .foreignTable(tc.getForeignTable())
                                .foreignColumn(tc.getForeignColumn())
                                .active(true)
                                .build())
                        .collect(Collectors.toList());
                columnRepository.saveAll(newColumns);
            }

            // Create an initial SchemaVersion
            SchemaVersionEntity schemaVersion = SchemaVersionEntity.builder()
                    .database(savedDb)
                    .build();
            versionService.createSchemaVersion(schemaVersion);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable("templateSchemas")
    public SchemaSearchResponse getTemplateSchemas() {
        List<DatabaseDto> databases = databaseService.getTemplateDatabases();
        List<TableDto> tables = new ArrayList<>();
        List<ColumnDto> columns = new ArrayList<>();

        if (databases.isEmpty()) return SchemaSearchResponse.builder().build();

        databases.forEach(d -> {
            tables.addAll(tableService.getTables(databaseMapper.toEntity(d)));
        });

        tables.forEach(t -> {
            columns.addAll(columnService.getColumns(tableMapper.toEntity(t)));
        });

        return SchemaSearchResponse.builder()
                .databases(databases)
                .tables(tables)
                .columns(columns)
                .build();
    }

    private boolean isVersionUsedInMessages(UUID databaseId, CustomUserDetails userDetails) {
        return messageService.isVersionUsedInMessages(databaseId, getCurrentDatabaseEntity(databaseId, userDetails).getCurrentVersion());
    }

    public Page<DatabaseDto> getDatabases(CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        return databaseService.getDatabases(userDetails, page, size, sort, direction);
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public DatabaseDto createDatabase(DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        return databaseService.createDatabase(databaseDTO, userDetails);
    }

    public DatabaseDto getDatabase(UUID databaseId, CustomUserDetails userDetails) {
        return databaseService.getDatabase(databaseId, userDetails);
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public DatabaseDto updateDatabase(UUID databaseId, DatabaseDto databaseDTO, CustomUserDetails userDetails) {
        return databaseService.updateDatabase(databaseId, databaseDTO, userDetails, isVersionUsedInMessages(databaseId, userDetails));
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public void deleteDatabase(UUID databaseId, CustomUserDetails userDetails) {
        databaseService.deleteDatabase(databaseId, userDetails);
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

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public TableDto createTable(UUID databaseId, TableDto tableDTO, CustomUserDetails userDetails) {
        return tableService.createTable(getCurrentDatabaseEntity(databaseId, userDetails), tableDTO, isVersionUsedInMessages(databaseId, userDetails));
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public TableDto updateTable(UUID databaseId, UUID tableId, TableDto tableDTO, CustomUserDetails userDetails) {
        return tableService.updateTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId, tableDTO, isVersionUsedInMessages(databaseId, userDetails));
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public void deleteTable(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        tableService.deleteTable(getCurrentDatabaseEntity(databaseId, userDetails), tableId, isVersionUsedInMessages(databaseId, userDetails));
    }

    private TableEntity getCurrentTableEntity(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return tableService.getCurrentTableEntity(getCurrentDatabaseEntity(databaseId, userDetails), tableId);
    }

    public List<ColumnDto> getColumns(UUID databaseId, UUID tableId, CustomUserDetails userDetails) {
        return columnService.getColumns(getCurrentTableEntity(databaseId, tableId, userDetails));
    }

    public ColumnDto getColumn(UUID databaseId, UUID tableId, UUID columnId, CustomUserDetails userDetails) {
        return columnService.getColumn(getCurrentTableEntity(databaseId, tableId, userDetails), columnId);
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public ColumnDto createColumn(UUID databaseId, UUID tableId, ColumnDto columnDto, CustomUserDetails userDetails) {
        return columnService.createColumn(getCurrentDatabaseEntity(databaseId, userDetails), getCurrentTableEntity(databaseId, tableId, userDetails), columnDto, isVersionUsedInMessages(databaseId, userDetails));
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public ColumnDto updateColumn(UUID databaseId, UUID tableId, UUID columnId, ColumnDto columnDto, CustomUserDetails userDetails) {
        return columnService.updateColumn(getCurrentDatabaseEntity(databaseId, userDetails), getCurrentTableEntity(databaseId, tableId, userDetails), columnId, columnDto, isVersionUsedInMessages(databaseId, userDetails));
    }

    @CacheEvict(value = "templateSchemas", allEntries = true)
    public void deleteColumn(UUID databaseId, UUID tableId, UUID columnId, CustomUserDetails userDetails) {
        columnService.deleteColumn(getCurrentDatabaseEntity(databaseId, userDetails), getCurrentTableEntity(databaseId, tableId, userDetails), columnId, isVersionUsedInMessages(databaseId, userDetails));
    }
}