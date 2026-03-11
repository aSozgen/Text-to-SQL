package com.texttosql.backend.service;

import com.texttosql.backend.dto.entity.ChatDto;
import com.texttosql.backend.dto.entity.MessageDto;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.util.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class ChatBotService {
    private final ChatService chatService;
    private final MessageService messageService;
    private final ExportService exportService;

    public Page<ChatDto> getChats(CustomUserDetails userDetails, int page, int size, String sort, String direction) {
        return chatService.getChats(userDetails, page, size, sort, direction);
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

    private ChatEntity getCurrentChatEntity(UUID chatId, CustomUserDetails userDetails) {
        return chatService.getCurrentChatEntity(chatId, userDetails);
    }

    public Page<MessageDto> getMessages(UUID chatID, CustomUserDetails userDetails, int page, int size) {
        return messageService.getMessages(getCurrentChatEntity(chatID, userDetails), page, size);
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

    public String exportChatToCsv(UUID chatID, CustomUserDetails userDetails) {
        return exportService.exportChatToCsv(getCurrentChatEntity(chatID, userDetails));
    }

    public String exportChatToMarkdown(UUID chatID, CustomUserDetails userDetails) {
        return exportService.exportChatToMarkdown(getCurrentChatEntity(chatID, userDetails));
    }

    public String exportChatToJson(UUID chatID, CustomUserDetails userDetails) {
        return exportService.exportChatToJson(getCurrentChatEntity(chatID, userDetails));
    }
}
