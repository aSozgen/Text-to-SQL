package com.texttosql.backend.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.texttosql.backend.controller.SearchController;
import com.texttosql.backend.dto.entity.*;
import com.texttosql.backend.dto.search.ChatSearchResponse;
import com.texttosql.backend.dto.search.SchemaSearchResponse;
import com.texttosql.backend.exception.GlobalExceptionHandler;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.SearchService;
import com.texttosql.backend.util.Feedback;
import com.texttosql.backend.util.SenderType;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class SearchTest {

    private MockMvc mockMvc;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private SearchController searchController;

    private CustomUserDetails mockUser;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchSchema_ShouldReturnResults_WhenQueryIsValid() throws Exception {
        String query = "user";
        LocalDateTime now = LocalDateTime.now();
        UUID dbId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        DatabaseDto databaseDto = new DatabaseDto(dbId, "UserDB", "Description", now);
        TableDto tableDto = new TableDto(tableId, dbId, "Users", "Description", now);
        ColumnDto columnDto = new ColumnDto(columnId, tableId, dbId, "username", "VARCHAR", false, now);

        SchemaSearchResponse response = SchemaSearchResponse.builder()
                .databases(Collections.singletonList(databaseDto))
                .tables(Collections.singletonList(tableDto))
                .columns(Collections.singletonList(columnDto))
                .build();

        when(searchService.searchSchema(
                any(CustomUserDetails.class),
                eq(query),
                anyInt(), // page
                anyInt(), // size
                anyString(), // sort
                anyString() // direction
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/search/schema")
                        .param("query", query)
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databases[0].databaseId").value(dbId.toString()))
                .andExpect(jsonPath("$.databases[0].name").value("UserDB"))
                .andExpect(jsonPath("$.tables[0].tableId").value(tableId.toString()))
                .andExpect(jsonPath("$.tables[0].name").value("Users"))
                .andExpect(jsonPath("$.columns[0].columnId").value(columnId.toString()))
                .andExpect(jsonPath("$.columns[0].name").value("username"));
    }

    @Test
    void searchChat_ShouldReturnResults_WhenQueryIsValid() throws Exception {
        String query = "hello";
        LocalDateTime now = LocalDateTime.now();
        UUID chatId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        ChatDto chatDto = new ChatDto(chatId, "Hello Chat", now);
        MessageDto messageDto = new MessageDto(
                messageId,
                chatId,
                null,
                "Hello World",
                95.0,
                SenderType.USER,
                Feedback.NONE,
                now
        );

        ChatSearchResponse response = ChatSearchResponse.builder()
                .chats(Collections.singletonList(chatDto))
                .messages(Collections.singletonList(messageDto))
                .build();

        when(searchService.searchChat(
                any(CustomUserDetails.class),
                eq(query),
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        )).thenReturn(response);

        mockMvc.perform(get("/api/v1/search/chatbot")
                        .param("query", query)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chats[0].chatId").value(chatId.toString()))
                .andExpect(jsonPath("$.chats[0].name").value("Hello Chat"))
                .andExpect(jsonPath("$.messages[0].messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.messages[0].content").value("Hello World"))
                .andExpect(jsonPath("$.messages[0].senderType").value("USER"));
    }

    @Test
    void searchSchema_ShouldReturnEmpty_WhenNoMatchFound() throws Exception {
        String query = "nonexistent";
        SchemaSearchResponse emptyResponse = SchemaSearchResponse.builder()
                .databases(Collections.emptyList())
                .tables(Collections.emptyList())
                .columns(Collections.emptyList())
                .build();

        when(searchService.searchSchema(any(), eq(query), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/search/schema")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databases").isEmpty())
                .andExpect(jsonPath("$.tables").isEmpty())
                .andExpect(jsonPath("$.columns").isEmpty());
    }

    @Test
    void searchChat_ShouldUseDefaultParams_WhenParamsNotProvided() throws Exception {
        String query = "test";
        ChatSearchResponse emptyResponse = ChatSearchResponse.builder()
                .chats(Collections.emptyList())
                .messages(Collections.emptyList())
                .build();

        when(searchService.searchChat(
                any(),
                eq(query),
                eq(0),
                eq(5),
                eq("createdAt"),
                eq("desc")
        )).thenReturn(emptyResponse);

        mockMvc.perform(get("/api/v1/search/chatbot")
                        .param("query", query))
                .andExpect(status().isOk());
    }
}