package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ColumnEntity;
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
public interface ColumnRepository extends JpaRepository<ColumnEntity, UUID> {
    Optional<ColumnEntity> findByTableAndColumnIdAndActiveTrue(TableEntity table, UUID columnId);

    List<ColumnEntity> findByTableAndActiveTrueOrderByCreatedAtDesc(TableEntity table);

    boolean existsByNameIgnoreCaseAndTableAndActiveTrue(String name, TableEntity table);

    @Query("SELECT c FROM ColumnEntity c WHERE c.table.database.user = :user AND c.active = true AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.dataType) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ColumnEntity> searchColumns(UserEntity user, String query, Pageable pageable);

    long countAllByTableAndActiveTrue(TableEntity table);
}
