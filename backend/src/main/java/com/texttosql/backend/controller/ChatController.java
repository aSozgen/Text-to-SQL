package com.texttosql.backend.controller;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.service.ChatService;
import com.texttosql.backend.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MessageService messageService;

    @GetMapping("/chats")
    public ResponseEntity<List<ChatDto>> getChats() {
        return ResponseEntity.ok(chatService.getChats());
    }

    @GetMapping("/chats/{chatID}")
    public ResponseEntity<ChatDto> getChat(@PathVariable UUID chatID) {
        return ResponseEntity.ok(chatService.getChat(chatID));
    }

    @PostMapping("/chats")
    public ResponseEntity<ChatDto> createChat(@Valid @RequestBody ChatDto chatDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatService.createChat(chatDto));
    }

    @PatchMapping("/chats/{chatID}")
    public ResponseEntity<ChatDto> updateChat(@PathVariable UUID chatID, @Valid @RequestBody ChatDto chatDto) {
        return ResponseEntity.ok(chatService.updateChat(chatID, chatDto));
    }

    @DeleteMapping("/chats/{chatID}")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID chatID) {
        chatService.deleteChat(chatID);
        return ResponseEntity.noContent().build();
    }

    private ChatEntity getCurrentChat(UUID chatID) {
        return chatService.getCurrentChatEntity(chatID);
    }

    @GetMapping("/chats/{chatID}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable UUID chatID) {
        return ResponseEntity.ok(messageService.getMessages(getCurrentChat(chatID)));
    }

    @PostMapping("/chats/{chatID}/messages")
    public ResponseEntity<MessageDto> createMessage(@PathVariable UUID chatID, @Valid @RequestBody MessageDto messageDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.createMessage(getCurrentChat(chatID), messageDto));
    }

    @PatchMapping("/chats/{chatID}/messages/{messageID}")
    public ResponseEntity<MessageDto> updateMessage(@PathVariable UUID messageID, @Valid @RequestBody MessageDto messageDto) {
        return ResponseEntity.ok(messageService.updateMessage(messageID, messageDto));
    }

    @DeleteMapping("/chats/{chatID}/messages/{messageID}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID messageID) {
        messageService.deleteMessage(messageID);
        return ResponseEntity.noContent().build();
    }
}
