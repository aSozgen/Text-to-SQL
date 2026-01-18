package com.texttosql.backend.repository;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<DatabaseEntity, UUID> {
    Optional<DatabaseEntity> findByUserAndDatabaseIdAndActiveTrue(UserEntity user, UUID databaseId);

    Page<DatabaseEntity> findByUserAndActiveTrue(UserEntity user, Pageable pageable);

    boolean existsByNameIgnoreCaseAndUserAndActiveTrue(String username, UserEntity user);

    @Query("SELECT d FROM DatabaseEntity d WHERE d.user = :user AND d.active = true AND " +
            "(LOWER(d.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<DatabaseEntity> searchDatabases(UserEntity user, String query, Pageable pageable);

    long countAllByUserAndActiveTrue(UserEntity user);
}
