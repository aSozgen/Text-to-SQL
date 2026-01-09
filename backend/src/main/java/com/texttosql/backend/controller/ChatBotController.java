package com.texttosql.backend.controller;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.ChatBotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final ChatBotService chatBotService;

    @GetMapping("/chats")
    public ResponseEntity<List<ChatDto>> getChats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.getChats(userDetails));
    }

    @GetMapping("/chats/{chatID}")
    public ResponseEntity<ChatDto> getChat(@PathVariable UUID chatID,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.getChat(chatID, userDetails));
    }

    @PostMapping("/chats")
    public ResponseEntity<ChatDto> createChat(@Valid @RequestBody ChatDto chatDto,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatBotService.createChat(chatDto, userDetails));
    }

    @PatchMapping("/chats/{chatID}")
    public ResponseEntity<ChatDto> updateChat(@PathVariable UUID chatID,
                                              @Valid @RequestBody ChatDto chatDto,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.updateChat(chatID, chatDto, userDetails));
    }

    @DeleteMapping("/chats/{chatID}")
    public ResponseEntity<Void> deleteChat(@PathVariable UUID chatID,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatBotService.deleteChat(chatID, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chats/{chatID}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable UUID chatID,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.getMessages(chatID, userDetails));
    }

    @PostMapping("/chats/{chatID}/messages")
    public ResponseEntity<MessageDto> createMessage(@PathVariable UUID chatID,
                                                    @Valid @RequestBody MessageDto messageDto,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatBotService.createMessage(chatID, messageDto, userDetails));
    }

    @PatchMapping("/chats/{chatID}/messages/{messageID}")
    public ResponseEntity<MessageDto> updateMessage(@PathVariable UUID chatID,
                                                    @PathVariable UUID messageID,
                                                    @Valid @RequestBody MessageDto messageDto,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.updateMessage(chatID, messageID, messageDto, userDetails));
    }

    @DeleteMapping("/chats/{chatID}/messages/{messageID}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID chatID,
                                              @PathVariable UUID messageID,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatBotService.deleteMessage(chatID, messageID, userDetails);
        return ResponseEntity.noContent().build();
    }
}