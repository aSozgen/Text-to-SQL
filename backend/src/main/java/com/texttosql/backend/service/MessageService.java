package com.texttosql.backend.service;

import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.dto.llm.ConversationTurn;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.exception.NotResourceOwnerException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.MessageRepository;
import com.texttosql.backend.util.SecurityUtil;
import com.texttosql.backend.util.SenderTypeEnum;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final SecurityUtil securityUtil;

    @Value("${llm.conversation-turn:2}")
    private int conversationTurns;

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(ChatEntity chatEntity) {
        checkResourceOwner(chatEntity);

        List<MessageEntity> messageEntities = messageRepository.findByChatIdAndActiveTrueOrderByCreatedAtDesc(chatEntity);
        return messageEntities.stream()
                .map(entity -> new MessageDto(entity.getMessageId(), entity.getContent(), entity.getSchema(),
                        entity.getSenderType(), entity.getFeedback()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationTurn> getHistoryForLlm(ChatEntity chatEntity) {
        int maxMessages = conversationTurns * 2;

        List<MessageEntity> messages = messageRepository.findByChatIdAndActiveTrueOrderByCreatedAtDesc(chatEntity);

        List<MessageEntity> recentMessages = messages.stream()
                .limit(maxMessages)
                .collect(Collectors.toList());
        Collections.reverse(recentMessages);

        List<ConversationTurn> history = new ArrayList<>();
        String lastUserContent = null;

        for (MessageEntity msg : recentMessages) {
            if (msg.getSenderType() == SenderTypeEnum.USER) {
                lastUserContent = msg.getContent();
            } else if (msg.getSenderType() == SenderTypeEnum.LLM && lastUserContent != null) {
                history.add(new ConversationTurn(lastUserContent, msg.getContent()));
                lastUserContent = null;
            }
        }

        return history;
    }

    @Transactional
    public @Valid MessageDto createMessage(ChatEntity chatEntity, MessageDto messageDto) {
        checkResourceOwner(chatEntity);

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setChatId(chatEntity);
        messageEntity.setSchema(messageDto.getSchema());
        messageEntity.setContent(messageDto.getContent());
        messageEntity.setSenderType(SenderTypeEnum.USER);

        MessageEntity savedMessageEntity = messageRepository.save(messageEntity);
        messageDto.setMessageId(savedMessageEntity.getMessageId());
        return messageDto;
    }

    @Transactional
    public MessageDto createSystemMessage(ChatEntity chat, String sql, Map<String, Object> schema) {
        MessageEntity message = MessageEntity.builder()
                .chatId(chat)
                .schema(schema)
                .content(sql)
                .senderType(SenderTypeEnum.LLM)
                .build();

        MessageEntity savedEntity =  messageRepository.save(message);
        return new MessageDto(savedEntity.getMessageId(), savedEntity.getContent(), savedEntity.getSchema(),
                savedEntity.getSenderType(), savedEntity.getFeedback());
    }

    public @Valid MessageDto updateMessage(ChatEntity chatEntity, UUID messageId, MessageDto messageDto) {
        checkResourceOwner(chatEntity);
        MessageEntity oldEntity = getCurrentMessageEntity(messageId);
        checkResourceOwner(oldEntity.getChatId());

        oldEntity.setSchema(messageDto.getSchema());
        oldEntity.setContent(messageDto.getContent());
        oldEntity.setFeedback(messageDto.getFeedback());

        messageRepository.save(oldEntity);
        messageDto.setMessageId(messageId);
        return messageDto;
    }

    @Transactional
    public void deleteMessage(UUID messageId) {
        MessageEntity messageEntity = getCurrentMessageEntity(messageId);
        checkResourceOwner(messageEntity.getChatId());

        messageEntity.setActive(false);
        messageRepository.save(messageEntity);
    }

    private MessageEntity getCurrentMessageEntity(UUID messageId) {
        return messageRepository.findByMessageIdAndActiveTrue(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
    }

    private void checkResourceOwner(ChatEntity chatEntity) {
        if (!securityUtil.isResourceOwner(chatEntity.getUserId().getUserId())) {
            throw new NotResourceOwnerException("User is not the owner of the resource");
        }
    }
}
