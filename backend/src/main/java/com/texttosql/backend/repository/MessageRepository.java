package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.util.FeedbackEnum;
import com.texttosql.backend.util.SenderTypeEnum;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Optional<MessageEntity> findByMessageId(UUID messageId);
    Optional<MessageEntity> findByMessageIdAndActiveTrue(UUID messageId);
    List<MessageEntity> findByChatIdOrderByCreatedAtDesc(ChatEntity chatId);
    List<MessageEntity> findByChatIdAndActiveTrueOrderByCreatedAtDesc(ChatEntity chatId);
    List<MessageEntity> findByChatIdAndActiveTrueOrderByCreatedAtAsc(ChatEntity chatId);
    List<MessageEntity> findByChatIdAndActiveTrueOrderByCreatedAtDesc(ChatEntity chat, Pageable pageable);
    List<MessageEntity> findByChatIdAndSenderTypeOrderByCreatedAtDesc(ChatEntity chatId, SenderTypeEnum senderType);
    List<MessageEntity> findByChatIdAndSenderTypeAndActiveTrueOrderByCreatedAtDesc(ChatEntity chatId, SenderTypeEnum senderType);
    List<MessageEntity> findByChatIdAndFeedbackOrderByCreatedAtDesc(ChatEntity chatId, FeedbackEnum feedback);
    List<MessageEntity> findByChatIdAndFeedbackAndActiveTrueOrderByCreatedAtDesc(ChatEntity chatId, FeedbackEnum feedback);
    List<MessageEntity> findBySenderTypeOrderByCreatedAtDesc(SenderTypeEnum senderType);
    List<MessageEntity> findBySenderTypeAndActiveTrueOrderByCreatedAtDesc(SenderTypeEnum senderType);
    List<MessageEntity> findByFeedbackOrderByCreatedAtDesc(FeedbackEnum feedback);
    List<MessageEntity> findByFeedbackAndActiveTrueOrderByCreatedAtDesc(FeedbackEnum feedback);
    List<MessageEntity> findByChatIdAndActiveTrueAndContentContainingIgnoreCaseOrderByCreatedAtDesc(ChatEntity chatId, String content);
    List<MessageEntity> findByChatIdAndActiveTrueAndSenderTypeAndContentContainingIgnoreCaseOrderByCreatedAtDesc(ChatEntity chatId, SenderTypeEnum senderType, String content);
    long countAllByChatId(ChatEntity chatId);
    long countAllByFeedback(FeedbackEnum feedback);
}
