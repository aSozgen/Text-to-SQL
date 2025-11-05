package com.texttosql.backend.repository;

import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUserId(UUID userId);
    Optional<UserEntity> findByUserIdAndActiveTrue(UUID userId);
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByUsernameAndActiveTrue(String username);
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByEmailAndActiveTrue(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserEntity> findByActiveTrueOrderByCreatedAtDesc();
}
