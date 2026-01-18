package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, UUID> {
    Optional<ChatEntity> findByChatIdAndUserAndActiveTrue(UUID chatId, UserEntity user);

    Page<ChatEntity> findByUserAndActiveTrue(UserEntity entity, Pageable pageable);

    Page<ChatEntity> findByUserAndActiveTrueAndNameContainingIgnoreCase(UserEntity user, String name, Pageable pageable);

    boolean existsByNameIgnoreCaseAndUserAndActiveTrue(String name, UserEntity user);

    long countAllByUserAndActiveTrue(UserEntity user);
}
