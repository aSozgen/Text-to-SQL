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
    Optional<ChatEntity> findByUserIdAndActiveTrueAndNameLikeIgnoreCase(UserEntity userId, String name);
    List<ChatEntity> findByUserIdOrderByCreatedAtAsc(UserEntity userId);
    List<ChatEntity> findByUserIdAndActiveTrueOrderByCreatedAtAsc(UserEntity userId);
    List<ChatEntity> findByUserIdAndActiveTrueAndNameLike(UserEntity userId, String name);
    boolean existsByNameAndUserIdAndActiveTrue(String name, UserEntity userId);
    boolean isActive(UUID chatId);
    long countAllByUserId(UserEntity userId);
    long countAllByUserIdAndActiveTrue(UserEntity userId);
}
