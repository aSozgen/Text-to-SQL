package com.texttosql.backend.service;

import com.texttosql.backend.entity.ColumnEntity;
import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.SchemaVersionEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.ColumnRepository;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.repository.SchemaVersionRepository;
import com.texttosql.backend.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemaVersionService {

    private final SchemaVersionRepository versionRepository;
    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;

    @Transactional(readOnly = true)
    public SchemaVersionEntity getSchemaVersion(UUID databaseId) throws ResourceNotFoundException {
        DatabaseEntity entity = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found."));
        return versionRepository.findByDatabaseAndVersionNumber(entity, entity.getCurrentVersion())
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found."));
    }

    @Transactional
    public void createSchemaVersion(SchemaVersionEntity schemaVersion) {
        schemaVersion.setSchemaStructure(getSchemaStructure(schemaVersion.getDatabase().getDatabaseId()));
        versionRepository.save(schemaVersion);
    }

    @Transactional
    public void updateSchemaVersion(DatabaseEntity entity) {
        SchemaVersionEntity schemaVersionEntity = getSchemaVersion(entity.getDatabaseId());
        Map<String, Object> schemaStructure = getSchemaStructure(entity.getDatabaseId());
        schemaVersionEntity.setSchemaStructure(schemaStructure);
        versionRepository.save(schemaVersionEntity);
    }

    public Map<String, Object> getSchemaStructure(UUID databaseId) {
        DatabaseEntity entity = databaseRepository.findById(databaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Database not found."));

        List<TableEntity> tables = tableRepository.findByDatabaseAndActiveTrueOrderByCreatedAtDesc(entity);

        Map<String, Object> tablesMap = new HashMap<>();

        for (TableEntity table : tables) {
            Map<String, Object> tableDetails = new HashMap<>();

            List<ColumnEntity> columns = columnRepository.findByTableAndActiveTrueOrderByCreatedAtDesc(table);
            List<Map<String, Object>> columnsList = columns.stream().map(column -> {
                Map<String, Object> colMap = new HashMap<>();
                colMap.put("columnName", column.getName());
                colMap.put("dataType", column.getDataType() != null ? column.getDataType() : "unknown");
                colMap.put("isPrimaryKey", column.isPrimaryKey());

                if (column.getForeignTable() != null && !column.getForeignTable().isEmpty() &&
                        column.getForeignColumn() != null && !column.getForeignColumn().isEmpty()) {
                    colMap.put("foreignTable", column.getForeignTable());
                    colMap.put("foreignColumn", column.getForeignColumn());
                }

                return colMap;
            }).collect(Collectors.toList());

            tableDetails.put("columns", columnsList);

            tablesMap.put(table.getName(), tableDetails);
        }

        Map<String, Object> schemaStructure = new HashMap<>();
        schemaStructure.put("tables", tablesMap);

        return schemaStructure;
    }

    @Transactional
    public void createOrUpdateSchemaSnapshot(DatabaseEntity database, boolean versionUsedInMessages) {
        if (versionUsedInMessages) {
            int newVersion = database.getCurrentVersion() + 1;
            database.setCurrentVersion(newVersion);
            databaseRepository.save(database);

            SchemaVersionEntity newSchemaVersion = SchemaVersionEntity.builder()
                    .database(database)
                    .versionNumber(newVersion)
                    .build();
            createSchemaVersion(newSchemaVersion);
        } else {
            updateSchemaVersion(database);
        }
    }
}