package com.texttosql.backend.service;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.mapper.ChatMapper;
import com.texttosql.backend.mapper.UserMapper;
import com.texttosql.backend.repository.ChatRepository;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<ChatDto> getChats(CustomUserDetails userDetails) {
        List<ChatEntity> chatEntities = chatRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(userMapper.toEntity(userDetails));
        return chatMapper.toDtoList(chatEntities);
    }

    @Transactional(readOnly = true)
    public ChatDto getChat(UUID chatId, CustomUserDetails userDetails) {
        ChatEntity chatEntity = getCurrentChatEntity(chatId, userDetails);
        return chatMapper.toDto(chatEntity);
    }

    public ChatDto createChat(ChatDto chatDto, CustomUserDetails userDetails) {

        if (chatRepository.existsByNameIgnoreCaseAndUserAndActiveTrue(chatDto.getName(), userMapper.toEntity(userDetails))) {
            throw new DuplicatedResourceException("There is already a Chat with the name '" + chatDto.getName() + "'");
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
            throw new DuplicatedResourceException("There is already a Chat with the name '" + chatDto.getName() + "'");
        }

        chatEntity.setName(chatDto.getName());

        chatRepository.save(chatEntity);
        chatDto.setChatId(chatEntity.getChatId());
        return chatDto;
    }

    @Transactional
    public void deleteChat(UUID chatId, CustomUserDetails userDetails) {
        ChatEntity chatEntity = getCurrentChatEntity(chatId, userDetails);
        chatEntity.setActive(false);

        chatRepository.save(chatEntity);
    }

    public ChatEntity getCurrentChatEntity(UUID chatId, CustomUserDetails userDetails) {
        return chatRepository.findByChatIdAndUserAndActiveTrue(chatId, userMapper.toEntity(userDetails))
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
    }
}
