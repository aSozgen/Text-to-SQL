package com.texttosql.backend.service;

import com.texttosql.backend.client.LlmClient;
import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.dto.llm.ConversationTurn;
import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.entity.SchemaVersionEntity;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.MessageMapper;
import com.texttosql.backend.repository.MessageRepository;
import com.texttosql.backend.util.SenderType;
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
    private final MessageMapper messageMapper;
    private final SchemaVersionService versionService;

    @Value("${llm.service.conversation-turn}")
    private int conversationTurns;

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(ChatEntity chat) {

        List<MessageEntity> entities = messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chat);
        return messageMapper.toDtoList(entities);
    }

    @Transactional(readOnly = true)
    public List<ConversationTurn> getHistoryForLlm(ChatEntity chat, SchemaVersionEntity schemaVersion) {
        int maxMessages = conversationTurns * 2;

        Pageable pageable = PageRequest.of(0, maxMessages);
        List<MessageEntity> messages = messageRepository.findByChatAndSchemaVersionAndActiveTrueOrderByCreatedAtDesc(chat, schemaVersion, pageable);

        if (messages.isEmpty()) {
            return null;
        }

        Collections.reverse(messages);

        List<ConversationTurn> history = new ArrayList<>();
        String lastUserMessage = null;

        for (MessageEntity msg : messages) {
            if (msg.getSenderType() == SenderType.USER) {
                lastUserMessage = msg.getContent();
            } else if (msg.getSenderType() == SenderType.LLM && lastUserMessage != null) {
                history.add(new ConversationTurn(lastUserMessage, msg.getContent()));
                lastUserMessage = null;
            }
        }

        return history;
    }

    @Transactional
    public MessageDto createMessage(ChatEntity chatEntity, MessageDto messageDto) {
        SchemaVersionEntity schemaVersion = versionService.getSchemaVersion(messageDto.getDatabaseId());
        List<ConversationTurn> history = getHistoryForLlm(chatEntity, schemaVersion);
        LLMRequest request = new LLMRequest(
                messageDto.getContent(),
                schemaVersion.getSchemaStructure(),
                history
        );

        LLMResponse response = llmClient.generateSql(request);
        MessageEntity userMessage = MessageEntity.builder()
                .chat(chatEntity)
                .schemaVersion(schemaVersion)
                .content(messageDto.getContent())
                .build();

        MessageEntity llmMessage = MessageEntity.builder()
                .chat(chatEntity)
                .schemaVersion(schemaVersion)
                .content(response.sql())
                .confidence(response.confidence())
                .senderType(SenderType.LLM)
                .build();

        messageRepository.save(userMessage);
        MessageEntity savedLlmMessage = messageRepository.save(llmMessage);

        return messageMapper.toDto(savedLlmMessage);
    }

    @Transactional
    public MessageDto updateMessage(ChatEntity chatEntity, UUID messageId, MessageDto messageDto) {
        MessageEntity entity = getCurrentMessageEntity(chatEntity, messageId);

        if (entity.getSenderType() == SenderType.LLM) {
            throw new IllegalStateException("LLM messages cannot be updated");
        }

        entity.setSchemaVersion(versionService.getSchemaVersion(messageDto.getDatabaseId()));
        entity.setContent(messageDto.getContent());
        entity.setFeedback(messageDto.getFeedback());

        messageRepository.save(entity);
        messageDto.setMessageId(messageId);
        return messageDto;
    }

    @Transactional
    public void deleteMessage(ChatEntity chat, UUID messageId) {
        MessageEntity entity = getCurrentMessageEntity(chat, messageId);

        entity.setActive(false);
        messageRepository.save(entity);
    }

    private MessageEntity getCurrentMessageEntity(ChatEntity chat, UUID messageId) {
        return messageRepository.findByChatAndMessageIdAndActiveTrue(chat, messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    public boolean isVersionUsedInMessages(UUID databaseId, int currentVersion) {
        return messageRepository.isVersionUsedInMessages(databaseId, currentVersion);
    }
}
