package com.texttosql.backend.repository;

import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.utils.FeedbackEnum;
import com.texttosql.backend.utils.SenderTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    Optional<MessageEntity> findByMessageId(UUID messageId);
    Optional<MessageEntity> findByMessageIdAndActiveTrue(UUID messageId);
    List<MessageEntity> findByChatIdOrderByCreatedAtAsc(ChatEntity chatId);
    List<MessageEntity> findByChatIdAndActiveTrueOrderByCreatedAtAsc(ChatEntity chatId);
    List<MessageEntity> findByChatIdAndSenderTypeOrderByCreatedAtAsc(ChatEntity chatId, SenderTypeEnum senderType);
    List<MessageEntity> findByChatIdAndSenderTypeAndActiveTrueOrderByCreatedAtAsc(ChatEntity chatId, SenderTypeEnum senderType);
    List<MessageEntity> findByChatIdAndFeedbackOrderByCreatedAtAsc(ChatEntity chatId, FeedbackEnum feedback);
    List<MessageEntity> findByChatIdAndFeedbackAndActiveTrueOrderByCreatedAtAsc(ChatEntity chatId, FeedbackEnum feedback);
    List<MessageEntity> findBySenderTypeOrderByCreatedAtAsc(SenderTypeEnum senderType);
    List<MessageEntity> findBySenderTypeAndActiveTrueOrderByCreatedAtAsc(SenderTypeEnum senderType);
    List<MessageEntity> findByFeedbackOrderByCreatedAtAsc(FeedbackEnum feedback);
    List<MessageEntity> findByFeedbackAndActiveTrueOrderByCreatedAtAsc(FeedbackEnum feedback);
    boolean isActive(UUID messageId);
    long countAllByChatId(ChatEntity chatId);
    long countAllByFeedback(FeedbackEnum feedback);
}
