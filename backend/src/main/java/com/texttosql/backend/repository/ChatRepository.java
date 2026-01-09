package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<ChatEntity, UUID> {
    Optional<ChatEntity> findByChatId(UUID chatId);
    Optional<ChatEntity> findByChatIdAndActiveTrue(UUID chatId);
    Optional<ChatEntity> findByChatIdAndUserAndActiveTrue(UUID chatId, UserEntity user);
    List<ChatEntity> findByUserOrderByCreatedAtDesc(UserEntity user);
    List<ChatEntity> findByUserAndActiveTrueOrderByCreatedAtDesc(UserEntity user);
    List<ChatEntity> findByUserAndActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(UserEntity user, String name);
    boolean existsByNameIgnoreCaseAndUserAndActiveTrue(String name, UserEntity user);
    long countAllByUserAndActiveTrue(UserEntity user);
}
