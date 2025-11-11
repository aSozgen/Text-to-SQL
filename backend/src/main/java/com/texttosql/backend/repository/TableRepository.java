package com.texttosql.backend.repository;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, UUID> {
    Optional<TableEntity> findByTableId(UUID tableId);
    List<TableEntity> findByDatabaseIdOrderByCreatedAtDesc(DatabaseEntity databaseId);
    List<TableEntity> findByDatabaseIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(DatabaseEntity databaseId, String name);
    Optional<TableEntity>  findByDatabaseIdAndNameIgnoreCase(DatabaseEntity databaseId, String name);
    boolean existsByNameIgnoreCaseAndDatabaseId(String name, DatabaseEntity databaseId);
    long countAllByDatabaseId(DatabaseEntity databaseId);
}
