package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.entity.SchemaVersionEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.util.Feedback;
import com.texttosql.backend.util.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Optional<MessageEntity> findByChatAndMessageIdAndActiveTrue(ChatEntity chat, UUID messageId);
    Page<MessageEntity> findByChatAndActiveTrueOrderByCreatedAtAsc(ChatEntity chat, Pageable pageable);
    List<MessageEntity> findByChatAndSchemaVersionAndActiveTrueOrderByCreatedAtDesc(
            ChatEntity chat,
            SchemaVersionEntity schemaVersion,
            Pageable pageable);

    Optional<MessageEntity> findFirstByChatAndCreatedAtGreaterThanEqualAndSenderTypeAndActiveTrueOrderByCreatedAtAsc(
            ChatEntity chat,
            LocalDateTime date,
            SenderType senderType
    );

    @Query("SELECT m FROM MessageEntity m WHERE m.chat.user = :user AND m.active = true AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<MessageEntity> searchMessages(UserEntity user, String query, Pageable pageable);


    @Query("SELECT COUNT(m) > 0 FROM MessageEntity m " +
            "WHERE m.schemaVersion.database.databaseId = :databaseId " +
            "AND m.schemaVersion.versionNumber = :currentVersion")
    boolean isVersionUsedInMessages(@Param("databaseId") UUID databaseId,
                                    @Param("currentVersion") int currentVersion);

    long countAllByChatAndActiveTrue(ChatEntity chat);
    long countAllByFeedback(Feedback feedback);
}
