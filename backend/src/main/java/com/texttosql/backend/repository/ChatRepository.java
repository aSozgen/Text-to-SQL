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
    List<ChatEntity> findByUserIdOrderByCreatedAtDesc(UserEntity userId);
    List<ChatEntity> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UserEntity userId);
    List<ChatEntity> findByUserIdAndActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(UserEntity userId, String name);
    boolean existsByNameIgnoreCaseAndUserIdAndActiveTrue(String name, UserEntity userId);
    long countAllByUserIdAndActiveTrue(UserEntity userId);
}
