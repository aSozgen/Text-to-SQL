package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.util.Feedback;
import com.texttosql.backend.util.SenderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Optional<MessageEntity> findByMessageId(UUID messageId);
    Optional<MessageEntity> findByMessageIdAndActiveTrue(UUID messageId);
    Optional<MessageEntity> findByChatAndMessageIdAndActiveTrue(ChatEntity chat, UUID messageId);
    List<MessageEntity> findByChatOrderByCreatedAtDesc(ChatEntity chat);
    List<MessageEntity> findByChatAndActiveTrueOrderByCreatedAtDesc(ChatEntity chat);
    List<MessageEntity> findByChatAndActiveTrueOrderByCreatedAtAsc(ChatEntity chat);
    List<MessageEntity> findByChatAndActiveTrueOrderByCreatedAtDesc(ChatEntity chat, Pageable pageable);
    List<MessageEntity> findByChatAndSenderTypeOrderByCreatedAtDesc(ChatEntity chat, SenderType senderType);
    List<MessageEntity> findByChatAndSenderTypeAndActiveTrueOrderByCreatedAtDesc(ChatEntity chat, SenderType senderType);
    List<MessageEntity> findByChatAndFeedbackOrderByCreatedAtDesc(ChatEntity chat, Feedback feedback);
    List<MessageEntity> findByChatAndFeedbackAndActiveTrueOrderByCreatedAtDesc(ChatEntity chat, Feedback feedback);
    List<MessageEntity> findBySenderTypeOrderByCreatedAtDesc(SenderType senderType);
    List<MessageEntity> findBySenderTypeAndActiveTrueOrderByCreatedAtDesc(SenderType senderType);
    List<MessageEntity> findByFeedbackOrderByCreatedAtDesc(Feedback feedback);
    List<MessageEntity> findByFeedbackAndActiveTrueOrderByCreatedAtDesc(Feedback feedback);
    List<MessageEntity> findByChatAndActiveTrueAndContentContainingIgnoreCaseOrderByCreatedAtDesc(ChatEntity chat, String content);
    List<MessageEntity> findByChatAndActiveTrueAndSenderTypeAndContentContainingIgnoreCaseOrderByCreatedAtDesc(ChatEntity chat, SenderType senderType, String content);
    long countAllByChatAndActiveTrue(ChatEntity chat);
    long countAllByFeedback(Feedback feedback);

    @Query("SELECT COUNT(m) > 0 FROM MessageEntity m " +
            "WHERE m.schemaVersion.database.databaseId = :databaseId " +
            "AND m.schemaVersion.versionNumber = :currentVersion")
    boolean isVersionUsedInMessages(@Param("databaseId") UUID databaseId,
                                    @Param("currentVersion") int currentVersion);
}
