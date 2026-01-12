package com.texttosql.backend.repository;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableRepository extends JpaRepository<TableEntity, UUID> {
    Optional<TableEntity> findByDatabaseAndTableIdAndActiveTrue(DatabaseEntity database, UUID tableId);
    List<TableEntity> findByDatabaseAndActiveTrueOrderByCreatedAtDesc(DatabaseEntity database);
    Page<TableEntity> findByDatabaseAndActiveTrue(DatabaseEntity databaseEntity, Pageable pageable);
    boolean existsByNameIgnoreCaseAndDatabaseAndActiveTrue(String name, DatabaseEntity database);
    long countAllByDatabaseAndActiveTrue(DatabaseEntity database);
}
