package com.texttosql.backend.repository;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.SchemaVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchemaVersionRepository extends JpaRepository<SchemaVersionEntity, UUID> {
    Optional<SchemaVersionEntity> findByDatabaseAndVersionNumber(DatabaseEntity database, int versionNumber);

}
