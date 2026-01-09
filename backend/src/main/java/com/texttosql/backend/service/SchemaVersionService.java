package com.texttosql.backend.service;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.SchemaVersionEntity;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.DatabaseRepository;
import com.texttosql.backend.repository.SchemaVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchemaVersionService {

    private final SchemaVersionRepository versionRepository;
    private final DatabaseRepository databaseRepository;

    @Transactional(readOnly = true)
    public SchemaVersionEntity getSchemaVersion(DatabaseEntity entity) throws ResourceNotFoundException {
        return versionRepository.findByDatabaseAndVersionNumber(entity, entity.getCurrentVersion())
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));
    }

    @Transactional
    public void createSchemaVersion(SchemaVersionEntity schemaVersion) {
        schemaVersion.setSchemaStructure(getSchemaStructure(schemaVersion.getDatabase()));
        versionRepository.save(schemaVersion);
    }

    @Transactional
    public void updateSchemaVersion(DatabaseEntity entity) {
        SchemaVersionEntity schemaVersionEntity = getSchemaVersion(entity);
        String schemaStructure = getSchemaStructure(entity);
        schemaVersionEntity.setSchemaStructure(schemaStructure);
        versionRepository.save(schemaVersionEntity);
    }

    // TODO
    public String getSchemaStructure(DatabaseEntity entity) {
        return "";
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
