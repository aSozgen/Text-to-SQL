package com.texttosql.backend.repository;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.TableEntity;
import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT t FROM TableEntity t WHERE t.database.user = :user AND t.active = true AND " +
            "(LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<TableEntity> searchTables(UserEntity user, String query, Pageable pageable);

    long countAllByDatabaseAndActiveTrue(DatabaseEntity database);
}
