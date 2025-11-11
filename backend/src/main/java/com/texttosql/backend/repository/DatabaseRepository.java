package com.texttosql.backend.repository;

import com.texttosql.backend.entity.DatabaseEntity;
import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatabaseRepository extends JpaRepository<DatabaseEntity, UUID> {
    Optional<DatabaseEntity> findByDatabaseId(UUID databaseId);
    List<DatabaseEntity> findByUserIdOrderByCreatedAtDesc(UserEntity userId);
    List<DatabaseEntity> findByUserIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(UserEntity userId, String name);
    Optional<DatabaseEntity> findByUserIdAndNameIgnoreCase(UserEntity userId, String name);
    boolean existsByNameIgnoreCaseAndUserId(String username, UserEntity userId);
    long countAllByUserId(UserEntity userId);
}
