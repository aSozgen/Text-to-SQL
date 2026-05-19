package com.texttosql.backend.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.texttosql.backend.controller.ChatBotController;
import com.texttosql.backend.dto.FeedbackRequest;
import com.texttosql.backend.dto.entity.ChatDto;
import com.texttosql.backend.dto.entity.MessageDto;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.GlobalExceptionHandler;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.ChatBotService;
import com.texttosql.backend.entity.enums.Feedback;
import com.texttosql.backend.entity.enums.SenderType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ChatBotTest {

    private MockMvc mockMvc;

    @Mock
    private ChatBotService chatBotService;

    @InjectMocks
    private ChatBotController chatBotController;

    private CustomUserDetails mockUser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        mockUser = new CustomUserDetails();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(@NonNull MethodParameter parameter, ModelAndViewContainer mavContainer, @NonNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(chatBotController)
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }


    @Test
    void createChat_ShouldReturnCreated_WhenRequestIsValid() throws Exception {
        UUID newChatId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChatDto requestDto = new ChatDto(null, "New Chat", null);
        ChatDto responseDto = new ChatDto(newChatId, "New Chat", now);

        when(chatBotService.createChat(any(ChatDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/chatbot/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chatId").value(newChatId.toString()))
                .andExpect(jsonPath("$.name").value("New Chat"))
                .andExpect(jsonPath("$.createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void createChat_ShouldReturnConflict_WhenNameExists() throws Exception {
        ChatDto requestDto = new ChatDto(null, "Existing Chat", null);

        when(chatBotService.createChat(any(ChatDto.class), any(CustomUserDetails.class)))
                .thenThrow(new DuplicatedResourceException("There is already a Chat with the same name."));

        mockMvc.perform(post("/api/v1/chatbot/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is already a Chat with the same name."));
    }

    @Test
    void getChats_ShouldReturnPage_WhenCalled() throws Exception {
        UUID chatId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChatDto chatDto = new ChatDto(chatId, "Test Chat", now);
        Page<ChatDto> chatPage = new PageImpl<>(Collections.singletonList(chatDto), PageRequest.of(0, 10), 1);

        when(chatBotService.getChats(any(CustomUserDetails.class), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(chatPage);

        mockMvc.perform(get("/api/v1/chatbot/chats")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].chatId").value(chatId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Test Chat"))
                .andExpect(jsonPath("$.content[0].createdAt").value(now.format(dateTimeFormatter)));

    }

    @Test
    void getChat_ShouldReturnChat() throws Exception {
        UUID chatId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChatDto chatDto = new ChatDto(chatId, "Single Chat", now);

        when(chatBotService.getChat(eq(chatId), any(CustomUserDetails.class)))
                .thenReturn(chatDto);

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}", chatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId").value(chatId.toString()))
                .andExpect(jsonPath("$.name").value("Single Chat"))
                .andExpect(jsonPath("$.createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void updateChat_ShouldReturnUpdatedChat() throws Exception {
        UUID chatId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ChatDto updateDto = new ChatDto(chatId, "Updated Name", null);
        ChatDto responseDto = new ChatDto(chatId, "Updated Name", now);

        when(chatBotService.updateChat(eq(chatId), any(ChatDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/chatbot/chats/{chatID}", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatId").value(chatId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void updateChat_ShouldReturnConflict_WhenNameAlreadyExists() throws Exception {
        UUID chatId = UUID.randomUUID();

        ChatDto updateDto = new ChatDto(chatId, "Existing Name", null);

        when(chatBotService.updateChat(eq(chatId), any(ChatDto.class), any(CustomUserDetails.class)))
                .thenThrow(new DuplicatedResourceException("There is already a Chat with the same name."));

        mockMvc.perform(patch("/api/v1/chatbot/chats/{chatID}", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is already a Chat with the same name."));
    }

    @Test
    void deleteChat_ShouldReturnNoContent() throws Exception {
        UUID chatId = UUID.randomUUID();
        doNothing().when(chatBotService).deleteChat(eq(chatId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/chatbot/chats/{chatID}", chatId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteChat_ShouldReturnNotFound_WhenChatDoesNotExist() throws Exception {
        UUID chatId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Chat not found."))
                .when(chatBotService).deleteChat(eq(chatId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/chatbot/chats/{chatID}", chatId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Chat not found."));
    }

    @Test
    void createMessage_ShouldReturnCreatedMessage() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID databaseId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MessageDto requestDto = new MessageDto(null, chatId, databaseId, "Hello Bot", null, SenderType.USER, Feedback.NONE, null);
        MessageDto responseDto = new MessageDto(messageId, chatId, databaseId, "Hello User", 95.0, SenderType.LLM, Feedback.NONE, now);

        when(chatBotService.createMessage(eq(chatId), any(MessageDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/chatbot/chats/{chatID}/messages", chatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.content").value("Hello User"))
                .andExpect(jsonPath("$.confidence").value(95.0))
                .andExpect(jsonPath("$.senderType").value("LLM"))
                .andExpect(jsonPath("$.feedback").value("NONE"))
                .andExpect(jsonPath("$.createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void getMessages_ShouldReturnMessagePage() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        UUID databaseId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MessageDto messageDto = new MessageDto(messageId, chatId, databaseId, "Hello Bot", -1.0, SenderType.USER, Feedback.NONE, now);

        Page<MessageDto> messagePage = new PageImpl<>(Collections.singletonList(messageDto), PageRequest.of(0, 10), 1);

        when(chatBotService.getMessages(eq(chatId), any(CustomUserDetails.class), anyInt(), anyInt()))
                .thenReturn(messagePage);

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}/messages", chatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.content[0].databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.content[0].content").value("Hello Bot"))
                .andExpect(jsonPath("$.content[0].confidence").value(-1.0))
                .andExpect(jsonPath("$.content[0].senderType").value(SenderType.USER.toString()))
                .andExpect(jsonPath("$.content[0].feedback").value("NONE"))
                .andExpect(jsonPath("$.content[0].createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void updateMessageContent_ShouldReturnUpdated() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        MessageDto updateDto = new MessageDto(messageId, chatId, null, "Edited Content", null, null, null, null);
        MessageDto responseDto = new MessageDto(messageId, chatId, null, "Edited Content", -1.0, SenderType.USER, Feedback.NONE, now);

        when(chatBotService.updateMessageContent(eq(chatId), eq(messageId), any(MessageDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/chatbot/chats/{chatID}/messages/{messageID}", chatId, messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.databaseId").value(nullValue()))
                .andExpect(jsonPath("$.content").value("Edited Content"))
                .andExpect(jsonPath("$.confidence").value(-1.0))
                .andExpect(jsonPath("$.senderType").value("USER"))
                .andExpect(jsonPath("$.feedback").value("NONE"))
                .andExpect(jsonPath("$.createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void updateMessageContent_ShouldReturnUnauthorized_WhenMessageIsLLM() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MessageDto updateDto = new MessageDto(messageId, chatId, null, "Illegal Edit", null, SenderType.LLM, null, null);

        when(chatBotService.updateMessageContent(eq(chatId), eq(messageId), any(MessageDto.class), any(CustomUserDetails.class)))
                .thenThrow(new AccessDeniedException("LLM messages cannot be updated."));

        mockMvc.perform(patch("/api/v1/chatbot/chats/{chatID}/messages/{messageID}", chatId, messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied: Authentication is required."));
    }

    @Test
    void updateMessageFeedback_ShouldReturnUpdatedMessage() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        FeedbackRequest feedbackRequest = new FeedbackRequest(Feedback.GOOD);
        LocalDateTime now = LocalDateTime.now();

        MessageDto responseDto = new MessageDto(messageId, chatId, null, "Content", 95.0, SenderType.LLM, Feedback.GOOD, now);

        when(chatBotService.updateMessageFeedback(eq(chatId), eq(messageId), eq(Feedback.GOOD), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/chatbot/chats/{chatID}/messages/{messageID}/feedback", chatId, messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedbackRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.databaseId").value(nullValue()))
                .andExpect(jsonPath("$.content").value("Content"))
                .andExpect(jsonPath("$.confidence").value(95.0))
                .andExpect(jsonPath("$.senderType").value("LLM"))
                .andExpect(jsonPath("$.feedback").value("GOOD"))
                .andExpect(jsonPath("$.createdAt").value(now.format(dateTimeFormatter)));
    }

    @Test
    void updateMessageFeedback_ShouldReturnUnauthorized_WhenMessageIsUser() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        FeedbackRequest feedbackRequest = new FeedbackRequest(Feedback.GOOD);

        when(chatBotService.updateMessageFeedback(eq(chatId), eq(messageId), eq(Feedback.GOOD), any(CustomUserDetails.class)))
                .thenThrow(new AccessDeniedException("Cannot give feedback for USER messages."));

        mockMvc.perform(patch("/api/v1/chatbot/chats/{chatID}/messages/{messageID}/feedback", chatId, messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feedbackRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied: Authentication is required."));
    }

    @Test
    void deleteMessage_ShouldReturnNoContent() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        doNothing().when(chatBotService).deleteMessage(eq(chatId), eq(messageId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/chatbot/chats/{chatID}/messages/{messageID}", chatId, messageId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMessage_ShouldReturnUnauthorized_WhenMessageIsLLM() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        doThrow(new AccessDeniedException("LLM messages cannot be deleted."))
                .when(chatBotService).deleteMessage(eq(chatId), eq(messageId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/chatbot/chats/{chatID}/messages/{messageID}", chatId, messageId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Access denied: Authentication is required."));
    }

    @Test
    void deleteMessage_ShouldReturnNotFound_WhenMessageDoesNotExist() throws Exception {
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Message not found."))
                .when(chatBotService).deleteMessage(eq(chatId), eq(messageId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/chatbot/chats/{chatID}/messages/{messageID}", chatId, messageId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Message not found."));
    }

    @Test
    void exportChatToCsv_ShouldReturnCsvFile() throws Exception {
        UUID chatId = UUID.randomUUID();
        String csvContent = "timestamp,sender,message\n2026-03-12 10:00:00,USER,Hello\n2026-03-12 10:01:00,ASSISTANT,Hi there";

        when(chatBotService.exportChatToCsv(eq(chatId), any(CustomUserDetails.class)))
                .thenReturn(csvContent);

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}/export/csv", chatId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(content().string(csvContent))
                .andExpect(header().exists("Content-Disposition"));

        verify(chatBotService).exportChatToCsv(eq(chatId), any(CustomUserDetails.class));
    }

    @Test
    void exportChatToMarkdown_ShouldReturnMarkdownFile() throws Exception {
        UUID chatId = UUID.randomUUID();
        String markdownContent = "# Chat Export\n\n## Messages\n\n**User**: Hello\n\n**Assistant**: Hi there";

        when(chatBotService.exportChatToMarkdown(eq(chatId), any(CustomUserDetails.class)))
                .thenReturn(markdownContent);

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}/export/markdown", chatId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(markdownContent))
                .andExpect(header().exists("Content-Disposition"));

        verify(chatBotService).exportChatToMarkdown(eq(chatId), any(CustomUserDetails.class));
    }

    @Test
    void exportChatToJson_ShouldReturnJsonFile() throws Exception {
        UUID chatId = UUID.randomUUID();
        String jsonContent = "{\"chatId\":\"" + chatId + "\",\"messages\":[{\"sender\":\"USER\",\"message\":\"Hello\"}]}";

        when(chatBotService.exportChatToJson(eq(chatId), any(CustomUserDetails.class)))
                .thenReturn(jsonContent);

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}/export/json", chatId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(jsonContent))
                .andExpect(header().exists("Content-Disposition"));

        verify(chatBotService).exportChatToJson(eq(chatId), any(CustomUserDetails.class));
    }

    @Test
    void exportChat_ShouldReturnUnauthorized_WhenUserNotOwner() throws Exception {
        UUID chatId = UUID.randomUUID();

        when(chatBotService.exportChatToCsv(eq(chatId), any(CustomUserDetails.class)))
                .thenThrow(new AccessDeniedException("You don't have access to this chat"));

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}/export/csv", chatId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportChat_ShouldReturnNotFound_WhenChatDoesNotExist() throws Exception {
        UUID chatId = UUID.randomUUID();

        when(chatBotService.exportChatToCsv(eq(chatId), any(CustomUserDetails.class)))
                .thenThrow(new ResourceNotFoundException("Chat not found"));

        mockMvc.perform(get("/api/v1/chatbot/chats/{chatID}/export/csv", chatId))
                .andExpect(status().isNotFound());
    }
}