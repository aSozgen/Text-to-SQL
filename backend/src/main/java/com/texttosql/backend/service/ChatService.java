package com.texttosql.backend.service;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.repository.ChatRepository;
import com.texttosql.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final SecurityUtil securityUtil;

    @Transactional(readOnly = true)
    public List<ChatDto> getChats() {
        List<ChatEntity> chatEntities = chatRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(getCurrentUserEntity());

        return chatEntities.stream()
                .map(entity -> new ChatDto(entity.getChatId(), entity.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatDto getChat(UUID chatId) {
        ChatEntity chatEntity = getCurrentChatEntity(chatId);

        return new ChatDto(chatEntity.getChatId(), chatEntity.getName());
    }


    public ChatDto createChat(ChatDto chatDto) {
        UserEntity currentUser = getCurrentUserEntity();

        if (chatRepository.existsByNameIgnoreCaseAndUserIdAndActiveTrue(chatDto.getName(), currentUser)) {
            throw new DuplicatedResourceException("There is already a Chat with the name '" + chatDto.getName() + "'");
        }

        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUserId(currentUser);
        chatEntity.setName(chatDto.getName());

        ChatEntity savedChatEntity = chatRepository.save(chatEntity);
        chatDto.setChatId(savedChatEntity.getChatId());
        return chatDto;
    }

    public ChatDto updateChat(UUID chatID, ChatDto chatDto) {
        ChatEntity chatEntity = getCurrentChatEntity(chatID);
        UserEntity currentUser = getCurrentUserEntity();

        if (chatRepository.existsByNameIgnoreCaseAndUserIdAndActiveTrue(chatDto.getName(), currentUser)
                && !chatEntity.getName().equalsIgnoreCase(chatDto.getName())) {
            throw new DuplicatedResourceException("There is already a Chat with the name '" + chatDto.getName() + "'");
        }

        chatEntity.setName(chatDto.getName());

        ChatEntity updatedEntity = chatRepository.save(chatEntity);
        chatDto.setChatId(updatedEntity.getChatId());
        return chatDto;
    }

    @Transactional
    public void deleteChat(UUID chatId) {
        ChatEntity chatEntity = getCurrentChatEntity(chatId);
        chatEntity.setActive(false);

        chatRepository.save(chatEntity);
    }

    private UserEntity getCurrentUserEntity() {
        return securityUtil.getCurrentUserEntity()
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public ChatEntity getCurrentChatEntity(UUID chatId) {
        return chatRepository.findByChatIdAndActiveTrue(chatId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat not found"));
    }
}
