package com.texttosql.backend.service;

import com.texttosql.backend.client.LlmClient;
import com.texttosql.backend.dto.entity.MessageDto;
import com.texttosql.backend.dto.llm.ConversationTurn;
import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.entity.SchemaVersionEntity;
import com.texttosql.backend.entity.enums.Feedback;
import com.texttosql.backend.entity.enums.SenderType;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.MessageMapper;
import com.texttosql.backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Page<MessageDto> getMessages(ChatEntity chat, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chat, pageable)
                .map(messageMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<ConversationTurn> getHistoryForLlm(ChatEntity chat, SchemaVersionEntity schemaVersion) {
        int maxMessages = conversationTurns * 2;
        Pageable pageable = PageRequest.of(0, maxMessages);

        List<MessageEntity> messages = messageRepository
                .findByChatAndSchemaVersionAndActiveTrueOrderByCreatedAtDesc(chat, schemaVersion, pageable);

        if (messages.isEmpty()) return null;

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
    public MessageDto createMessage(ChatEntity chat, MessageDto messageDto) {
        return createMessageWithTimestamp(chat, messageDto, null);
    }

    @Transactional
    public MessageDto createMessageWithTimestamp(ChatEntity chat, MessageDto messageDto, LocalDateTime createdAt) {
        SchemaVersionEntity schemaVersion = null;

        if (messageDto.getDatabaseId() != null) {
            schemaVersion = versionService.getSchemaVersion(messageDto.getDatabaseId());
        }

        // 1. Save user message immediately to ensure it persists
        MessageEntity userMessage = MessageEntity.builder()
                .chat(chat)
                .schemaVersion(schemaVersion)
                .content(messageDto.getContent())
                .senderType(SenderType.USER)
                .createdAt(createdAt)
                .build();
        messageRepository.save(userMessage);

        // 2. Prepare for LLM call with history
        List<ConversationTurn> history = getHistoryForLlm(chat, schemaVersion);
        String currentQuestion = messageDto.getContent();
        LLMResponse response = null;
        int maxRetries = 2;
        int attempt = 0;

        // Self-Correction Loop
        while (attempt <= maxRetries) {
            LLMRequest request = new LLMRequest(
                    currentQuestion,
                    schemaVersion != null ? schemaVersion.getSchemaStructure() : null,
                    history
            );

            try {
                response = llmClient.generateSql(request);

                if (response != null && Boolean.TRUE.equals(response.isValid())) {
                    break;
                }

                // If invalid, prepare for retry with feedback
                if (response != null && response.validationError() != null && attempt < maxRetries) {
                    currentQuestion = messageDto.getContent() + 
                        "\n\nIMPORTANT: Your previous SQL attempt was invalid.\nError: " + response.validationError() + 
                        "\nPlease fix the SQL query and provide only the corrected SQL.";
                }
            } catch (Exception e) {
                if (attempt == maxRetries) break; 
            }
            
            attempt++;
        }

        // Determine LLM message content
        String llmContent;
        Double confidence = -1.0;

        if (response != null && Boolean.TRUE.equals(response.isValid())) {
            llmContent = response.sql();
            confidence = response.confidence();
        } else {
            llmContent = "-- Error: I was unable to generate a valid SQL query for your request after multiple attempts.";
            if (response != null && response.validationError() != null) {
                llmContent += "\nReason: " + response.validationError();
            }
        }

        // 3. Save LLM response
        MessageEntity llmMessage = MessageEntity.builder()
                .chat(chat)
                .schemaVersion(schemaVersion)
                .content(llmContent)
                .confidence(confidence)
                .senderType(SenderType.LLM)
                .build();

        MessageEntity savedLlmMessage = messageRepository.save(llmMessage);

        return messageMapper.toDto(savedLlmMessage);
    }

    @Transactional
    public MessageDto updateMessageContent(ChatEntity chat, UUID messageId, MessageDto messageDto) {
        MessageEntity oldUserMessage = getCurrentMessageEntity(chat, messageId);

        if (oldUserMessage.getSenderType() == SenderType.LLM) {
            throw new AccessDeniedException("LLM messages cannot be updated.");
        }

        LocalDateTime originalTimestamp = oldUserMessage.getCreatedAt();

        oldUserMessage.setActive(false);
        messageRepository.save(oldUserMessage);

        findCorrespondingLlmMessage(chat, oldUserMessage).ifPresent(llmMsg -> {
            llmMsg.setActive(false);
            messageRepository.save(llmMsg);
        });

        return createMessageWithTimestamp(chat, messageDto, originalTimestamp);
    }

    @Transactional
    public MessageDto updateMessageFeedback(ChatEntity chat, UUID messageId, Feedback feedback) {
        MessageEntity message = getCurrentMessageEntity(chat, messageId);

        if (message.getSenderType() == SenderType.USER) {
            throw new AccessDeniedException("Cannot give feedback for USER messages.");
        }

        message.setFeedback(feedback);
        return messageMapper.toDto(messageRepository.save(message));
    }

    @Transactional
    public void deleteMessage(ChatEntity chat, UUID messageId) {
        MessageEntity entity = getCurrentMessageEntity(chat, messageId);

        if (entity.getSenderType() == SenderType.LLM) {
            throw new AccessDeniedException("LLM messages cannot be deleted.");
        }

        entity.setActive(false);
        messageRepository.save(entity);

        findCorrespondingLlmMessage(chat, entity).ifPresent(llmMsg -> {
            llmMsg.setActive(false);
            messageRepository.save(llmMsg);
        });
    }

    private Optional<MessageEntity> findCorrespondingLlmMessage(ChatEntity chat, MessageEntity userMessage) {
        return messageRepository.findFirstByChatAndCreatedAtGreaterThanEqualAndSenderTypeAndActiveTrueOrderByCreatedAtAsc(
                chat,
                userMessage.getCreatedAt(),
                SenderType.LLM
        );
    }

    private MessageEntity getCurrentMessageEntity(ChatEntity chat, UUID messageId) {
        return messageRepository.findByChatAndMessageIdAndActiveTrue(chat, messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found."));
    }

    public boolean isVersionUsedInMessages(UUID databaseId, int currentVersion) {
        return messageRepository.isVersionUsedInMessages(databaseId, currentVersion);
    }
}