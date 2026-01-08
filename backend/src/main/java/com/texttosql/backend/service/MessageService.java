package com.texttosql.backend.service;

import com.texttosql.backend.client.LlmClient;
import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.dto.llm.ConversationTurn;
import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.exception.NotResourceOwnerException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.MessageRepository;
import com.texttosql.backend.util.SecurityUtil;
import com.texttosql.backend.util.SenderTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final LlmClient llmClient;
    private final SecurityUtil securityUtil;

    @Value("${llm.service.conversation-turn}")
    private int conversationTurns;

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(ChatEntity chatEntity) {

        return messageRepository
                .findByChatIdAndActiveTrueOrderByCreatedAtAsc(chatEntity)
                .stream()
                .map(e -> new MessageDto(
                        e.getMessageId(),
                        e.getContent(),
                        e.getSchema(),
                        e.getConfidence(),
                        e.getSenderType(),
                        e.getFeedback()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationTurn> getHistoryForLlm(ChatEntity chatEntity) {
        int maxMessages = conversationTurns * 2;

        Pageable pageable = PageRequest.of(0, maxMessages);
        List<MessageEntity> messages =
                messageRepository.findByChatIdAndActiveTrueOrderByCreatedAtDesc(chatEntity, pageable);

        if (messages.isEmpty()) {
            return null;
        }

        Collections.reverse(messages);

        List<ConversationTurn> history = new ArrayList<>();
        String lastUserMessage = null;

        for (MessageEntity msg : messages) {
            if (msg.getSenderType() == SenderTypeEnum.USER) {
                lastUserMessage = msg.getContent();
            } else if (msg.getSenderType() == SenderTypeEnum.LLM && lastUserMessage != null) {
                history.add(new ConversationTurn(lastUserMessage, msg.getContent()));
                lastUserMessage = null;
            }
        }

        return history;
    }

    public MessageDto createMessage(ChatEntity chatEntity, MessageDto messageDto) {

        MessageEntity userMessage = new MessageEntity();
        userMessage.setChatId(chatEntity);
        userMessage.setSchema(messageDto.getSchema());
        userMessage.setContent(messageDto.getContent());
        userMessage.setSenderType(SenderTypeEnum.USER);
        messageRepository.save(userMessage);

        List<ConversationTurn> history = getHistoryForLlm(chatEntity);
        LLMRequest request = new LLMRequest(
                messageDto.getContent(),
                messageDto.getSchema(),
                history
        );

        LLMResponse response = llmClient.generateSql(request);

        return createLLMMessage(
                chatEntity,
                response.sql(),
                messageDto.getSchema(),
                response.confidence()
        );
    }

    @Transactional
    public MessageDto createLLMMessage(ChatEntity chat, String sql, String schema, Double confidence) {
        MessageEntity message = MessageEntity.builder()
                .chatId(chat)
                .schema(schema)
                .content(sql)
                .confidence(confidence)
                .senderType(SenderTypeEnum.LLM)
                .build();

        MessageEntity saved = messageRepository.save(message);

        return new MessageDto(
                saved.getMessageId(),
                saved.getContent(),
                saved.getSchema(),
                saved.getConfidence(),
                saved.getSenderType(),
                saved.getFeedback()
        );
    }

    @Transactional
    public MessageDto updateMessage(UUID messageId, MessageDto messageDto) {
        MessageEntity entity = getCurrentMessageEntity(messageId);

        if (entity.getSenderType() == SenderTypeEnum.LLM) {
            throw new IllegalStateException("LLM messages cannot be updated");
        }

        entity.setSchema(messageDto.getSchema());
        entity.setContent(messageDto.getContent());
        entity.setFeedback(messageDto.getFeedback());

        messageRepository.save(entity);
        messageDto.setMessageId(messageId);
        return messageDto;
    }

    @Transactional
    public void deleteMessage(UUID messageId) {
        MessageEntity entity = getCurrentMessageEntity(messageId);

        entity.setActive(false);
        messageRepository.save(entity);
    }

    private MessageEntity getCurrentMessageEntity(UUID messageId) {
        return messageRepository.findByMessageIdAndActiveTrue(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }
}
