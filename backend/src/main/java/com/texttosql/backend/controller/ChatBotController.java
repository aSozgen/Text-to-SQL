package com.texttosql.backend.controller;

import com.texttosql.backend.dto.FeedbackRequest;
import com.texttosql.backend.dto.entity.ChatDto;
import com.texttosql.backend.dto.entity.MessageDto;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.ChatBotService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Tag(name = "3. Chat Bot", description = "Chat interaction and message history management.")
public class ChatBotController {

    private final ChatBotService chatBotService;

    @GetMapping("/chats")
    public ResponseEntity<Page<ChatDto>> getChats(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(defaultValue = "createdAt") String sort,
                                                  @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(chatBotService.getChats(userDetails, page, size, sort, direction));
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
    public ResponseEntity<Page<MessageDto>> getMessages(@PathVariable UUID chatID,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(chatBotService.getMessages(chatID, userDetails, page, size));
    }

    @PostMapping("/chats/{chatID}/messages")
    public ResponseEntity<MessageDto> createMessage(@PathVariable UUID chatID,
                                                    @Valid @RequestBody MessageDto messageDto,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatBotService.createMessage(chatID, messageDto, userDetails));
    }

    @PatchMapping("/chats/{chatID}/messages/{messageID}")
    public ResponseEntity<MessageDto> updateMessageContent(@PathVariable UUID chatID,
                                                           @PathVariable UUID messageID,
                                                           @Valid @RequestBody MessageDto messageDto,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.updateMessageContent(chatID, messageID, messageDto, userDetails));
    }

    @PatchMapping("/chats/{chatID}/messages/{messageID}/feedback")
    public ResponseEntity<MessageDto> updateMessageFeedback(@PathVariable UUID chatID,
                                                            @PathVariable UUID messageID,
                                                            @Valid @RequestBody FeedbackRequest request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatBotService.updateMessageFeedback(chatID, messageID, request.feedback(), userDetails));
    }

    @DeleteMapping("/chats/{chatID}/messages/{messageID}")
    public ResponseEntity<Void> deleteMessage(@PathVariable UUID chatID,
                                              @PathVariable UUID messageID,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatBotService.deleteMessage(chatID, messageID, userDetails);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/chats/{chatID}/export/csv")
    public ResponseEntity<String> exportChatToCsv(@PathVariable UUID chatID,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        String csv = chatBotService.exportChatToCsv(chatID, userDetails);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "chat-" + chatID + ".csv");
        return ResponseEntity.ok().headers(headers).body(csv);
    }

    @GetMapping("/chats/{chatID}/export/markdown")
    public ResponseEntity<String> exportChatToMarkdown(@PathVariable UUID chatID,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        String markdown = chatBotService.exportChatToMarkdown(chatID, userDetails);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "chat-" + chatID + ".md");
        return ResponseEntity.ok().headers(headers).body(markdown);
    }

    @GetMapping("/chats/{chatID}/export/json")
    public ResponseEntity<String> exportChatToJson(@PathVariable UUID chatID,
                                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        String json = chatBotService.exportChatToJson(chatID, userDetails);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", "chat-" + chatID + ".json");
        return ResponseEntity.ok().headers(headers).body(json);
    }
}