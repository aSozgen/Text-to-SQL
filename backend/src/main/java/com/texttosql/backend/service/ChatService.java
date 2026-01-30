package com.texttosql.backend.service;

import com.texttosql.backend.dto.entity.ChatDto;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.ChatMapper;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.ChatRepository;
import com.texttosql.backend.repository.MessageRepository;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatMapper chatMapper;
    private final UserMapper userMapper;
    private final MessageRepository messageRepository;

    @Transactional(readOnly = true)
    public Page<ChatDto> getChats(CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<ChatEntity> chatEntities = chatRepository.findByUserAndActiveTrue(userMapper.toEntity(userDetails), pageable);
        return chatEntities.map(chatMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ChatDto getChat(UUID chatId, CustomUserDetails userDetails) {
        ChatEntity chatEntity = getCurrentChatEntity(chatId, userDetails);
        return chatMapper.toDto(chatEntity);
    }

    public ChatDto createChat(ChatDto chatDto, CustomUserDetails userDetails) {

        if (chatRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(chatDto.getName(), userMapper.toEntity(userDetails))) {
            throw new DuplicatedResourceException("There is already a Chat with the same name.");
        }

        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUser(userMapper.toEntity(userDetails));
        chatEntity.setName(chatDto.getName());

        ChatEntity savedChatEntity = chatRepository.save(chatEntity);
        chatDto.setChatId(savedChatEntity.getChatId());
        return chatDto;
    }

    public ChatDto updateChat(UUID chatID, ChatDto chatDto, CustomUserDetails userDetails) {
        ChatEntity chatEntity = getCurrentChatEntity(chatID, userDetails);

        if (chatRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(chatDto.getName(), userMapper.toEntity(userDetails))
                && !chatEntity.getName().equalsIgnoreCase(chatDto.getName())) {
            throw new DuplicatedResourceException("There is already a Chat with the same name.");
        }

        chatEntity.setName(chatDto.getName());

        chatRepository.save(chatEntity);
        chatDto.setChatId(chatEntity.getChatId());
        return chatDto;
    }

    @Transactional
    public void deleteChat(UUID chatId, CustomUserDetails userDetails) {
        ChatEntity chatEntity = getCurrentChatEntity(chatId, userDetails);

        List<MessageEntity> messages = messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chatEntity);

        if (!messages.isEmpty()) {
            messages.forEach(msg -> msg.setActive(false));
            messageRepository.saveAll(messages);
        }

        chatEntity.setActive(false);
        chatRepository.save(chatEntity);
    }

    public ChatEntity getCurrentChatEntity(UUID chatId, CustomUserDetails userDetails) {
        return chatRepository.findByChatIdAndUserAndActiveTrue(chatId, userMapper.toEntity(userDetails))
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found."));
    }
}
