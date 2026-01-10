package com.texttosql.backend.service;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.util.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ChatBotService {
    private final ChatService chatService;
    private final MessageService messageService;

    public List<ChatDto> getChats(CustomUserDetails userDetails) {
        return chatService.getChats(userDetails);
    }

    public ChatDto getChat(UUID chatID, CustomUserDetails userDetails) {
        return chatService.getChat(chatID, userDetails);
    }

    public ChatDto createChat(ChatDto chatDto, CustomUserDetails userDetails) {
        return chatService.createChat(chatDto, userDetails);
    }

    public ChatDto updateChat(UUID chatID, ChatDto chatDto, CustomUserDetails userDetails) {
        return chatService.updateChat(chatID, chatDto, userDetails);
    }

    public void deleteChat(UUID chatID, CustomUserDetails userDetails) {
        chatService.deleteChat(chatID, userDetails);
    }

    private ChatEntity getCurrentChatEntity(UUID chatId ,CustomUserDetails userDetails) {
        return chatService.getCurrentChatEntity(chatId, userDetails);
    }

    public List<MessageDto> getMessages(UUID chatID, CustomUserDetails userDetails) {
        return messageService.getMessages(getCurrentChatEntity(chatID, userDetails));
    }

    public MessageDto createMessage(UUID chatID, MessageDto messageDto, CustomUserDetails userDetails) {
        return messageService.createMessage(getCurrentChatEntity(chatID, userDetails), messageDto);
    }

    public MessageDto updateMessageContent(UUID chatID, UUID messageID, MessageDto messageDto, CustomUserDetails userDetails) {
        return messageService.updateMessageContent(getCurrentChatEntity(chatID, userDetails), messageID, messageDto);
    }

    public MessageDto updateMessageFeedback(UUID chatID, UUID messageID, Feedback feedback, CustomUserDetails userDetails) {
        return messageService.updateMessageFeedback(getCurrentChatEntity(chatID, userDetails), messageID, feedback);
    }

    public void deleteMessage(UUID chatID, UUID messageID, CustomUserDetails userDetails) {
        messageService.deleteMessage(getCurrentChatEntity(chatID, userDetails), messageID);
    }
}
