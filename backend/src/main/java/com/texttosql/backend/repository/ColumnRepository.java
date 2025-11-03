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
    Optional<ColumnEntity> findByColumnId(UUID columnId);
    List<ColumnEntity> findByTableIdOrderByCreatedAtAsc(TableEntity tableId);
    List<ColumnEntity> findByTableIdAndNameLikeIgnoreCase(TableEntity tableId, String name);
    boolean existsByNameAndTableId(String name, TableEntity tableId);
    long countAllByTableId(TableEntity tableId);
}
