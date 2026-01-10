package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ColumnEntity;
import com.texttosql.backend.entity.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ColumnRepository extends JpaRepository<ColumnEntity, UUID> {
    Optional<ColumnEntity> findByTableAndColumnIdAndActiveTrue(TableEntity table, UUID columnId);
    List<ColumnEntity> findByTableAndActiveTrueOrderByCreatedAtDesc(TableEntity table);
    boolean existsByNameIgnoreCaseAndTableAndActiveTrue(String name, TableEntity table);
    long countAllByTableAndActiveTrue(TableEntity table);
}
