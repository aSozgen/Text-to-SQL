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

import java.util.List;
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
        DatabaseEntity entity = databaseRepository.findById(databaseId).orElseThrow(() -> new ResourceNotFoundException("Database not found."));
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
        String schemaStructure = getSchemaStructure(entity.getDatabaseId());
        schemaVersionEntity.setSchemaStructure(schemaStructure);
        versionRepository.save(schemaVersionEntity);
    }

    public String getSchemaStructure(UUID databaseId) {
        DatabaseEntity entity = databaseRepository.findById(databaseId).orElseThrow(() -> new ResourceNotFoundException("Database not found."));
        List<TableEntity> tables = tableRepository.findByDatabaseAndActiveTrueOrderByCreatedAtDesc(entity);
        StringBuilder schemaBuilder = new StringBuilder();

        for (int i = 0; i < tables.size(); i++) {
            TableEntity table = tables.get(i);
            schemaBuilder.append(table.getName()).append(": ");
            List<ColumnEntity> columns = columnRepository.findByTableAndActiveTrueOrderByCreatedAtDesc(table);

            String columnsString = columns.stream()
                    .map(this::formatColumnString)
                    .collect(Collectors.joining(", "));

            schemaBuilder.append(columnsString);
            if (i != tables.size() - 1) {
                schemaBuilder.append(" | ");
            }
        }

        return schemaBuilder.toString().trim();
    }

    private String formatColumnString(ColumnEntity column) {
        String dataType = column.getDataType();
        String columnName = column.getName().toLowerCase();

        if (dataType == null) dataType = "unknown";

        StringBuilder sb = new StringBuilder();

        if (column.isPrimaryKey()) {
            sb.append(String.format("%s (%s) [PK]", columnName, dataType.toUpperCase()));
        } else {
            sb.append(String.format("%s (%s)", columnName, dataType.toLowerCase()));
        }

        if (column.getForeignTable() != null && column.getForeignColumn() != null) {
            sb.append(String.format(" [FK -> %s.%s]", column.getForeignTable(), column.getForeignColumn()));
        }

        return sb.toString();
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
