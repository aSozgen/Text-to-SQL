package com.texttosql.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.texttosql.backend.dto.SchemaImportRequest;
import com.texttosql.backend.dto.entity.ColumnDto;
import com.texttosql.backend.dto.entity.DatabaseDto;
import com.texttosql.backend.dto.entity.TableDto;
import com.texttosql.backend.exception.DuplicatedResourceException;
import com.texttosql.backend.exception.GlobalExceptionHandler;
import com.texttosql.backend.exception.ResourceNotFoundException;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.SchemaService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SchemaTest {

    private MockMvc mockMvc;

    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private SchemaController schemaController;

    private CustomUserDetails mockUser;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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

        mockMvc = MockMvcBuilders.standaloneSetup(schemaController)
                .setCustomArgumentResolvers(authPrincipalResolver, new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void importDatabase_ShouldReturnCreated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();

        List<Map<String, Object>> dummyJson = List.of(Map.of("table", "users", "columns", List.of()));

        SchemaImportRequest request = new SchemaImportRequest();
        request.setName("Imported_DB");
        request.setDescription("Description");
        request.setJsonContent(dummyJson);

        DatabaseDto responseDto = new DatabaseDto(databaseId, "Imported_DB", "Description", now);

        when(schemaService.importSchema(any(SchemaImportRequest.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/schemas/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.name").value("Imported_DB"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void getDatabases_ShouldReturnPage() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();

        DatabaseDto dbDto = new DatabaseDto(databaseId, "My_DB", "Test Desc", now);
        Page<DatabaseDto> page = new PageImpl<>(Collections.singletonList(dbDto), PageRequest.of(0, 10), 1);

        when(schemaService.getDatabases(any(CustomUserDetails.class), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/schemas/databases")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("My_DB"))
                .andExpect(jsonPath("$.content[0].description").value("Test Desc"))
                .andExpect(jsonPath("$.content[0].createdAt").value(now.format(formatter)));
    }

    @Test
    void getDatabase_ShouldReturnDatabase() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();

        DatabaseDto dbDto = new DatabaseDto(databaseId, "Single_DB", "Desc", now);

        when(schemaService.getDatabase(eq(databaseId), any(CustomUserDetails.class)))
                .thenReturn(dbDto);

        mockMvc.perform(get("/api/v1/schemas/databases/{id}", databaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.name").value("Single_DB"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void getDatabase_ShouldReturnNotFound_WhenNotExists() throws Exception {
        UUID databaseId = UUID.randomUUID();

        when(schemaService.getDatabase(eq(databaseId), any(CustomUserDetails.class)))
                .thenThrow(new ResourceNotFoundException("Database not found"));

        mockMvc.perform(get("/api/v1/schemas/databases/{id}", databaseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Database not found"));
    }

    @Test
    void createDatabase_ShouldReturnCreated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();

        DatabaseDto requestDto = new DatabaseDto(null, "New_DB", "Desc", null);
        DatabaseDto responseDto = new DatabaseDto(databaseId, "New_DB", "Desc", now);

        when(schemaService.createDatabase(any(DatabaseDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/schemas/databases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.name").value("New_DB"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void createDatabase_ShouldReturnConflict_WhenNameExists() throws Exception {
        DatabaseDto requestDto = new DatabaseDto(null, "Existing_DB", "Desc", null);

        when(schemaService.createDatabase(any(DatabaseDto.class), any(CustomUserDetails.class)))
                .thenThrow(new DuplicatedResourceException("There is already a Database with the name 'Existing_DB'"));

        mockMvc.perform(post("/api/v1/schemas/databases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is already a Database with the name 'Existing_DB'"));
    }

    @Test
    void updateDatabase_ShouldReturnUpdated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();

        DatabaseDto updateDto = new DatabaseDto(databaseId, "Updated_DB", "New Desc", null);
        DatabaseDto responseDto = new DatabaseDto(databaseId, "Updated_DB", "New Desc", now);

        when(schemaService.updateDatabase(eq(databaseId), any(DatabaseDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/schemas/databases/{id}", databaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.databaseId").value(databaseId.toString()))
                .andExpect(jsonPath("$.name").value("Updated_DB"))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void updateDatabase_ShouldReturnConflict_WhenNameExists() throws Exception {
        UUID databaseId = UUID.randomUUID();

        DatabaseDto updateDto = new DatabaseDto(databaseId, "Existing_Name", "Desc", null);

        when(schemaService.updateDatabase(eq(databaseId), any(DatabaseDto.class), any(CustomUserDetails.class)))
                .thenThrow(new DuplicatedResourceException("There is already a Database with the name 'Existing_Name'"));

        mockMvc.perform(patch("/api/v1/schemas/databases/{id}", databaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is already a Database with the name 'Existing_Name'"));
    }

    @Test
    void deleteDatabase_ShouldReturnNoContent() throws Exception {
        UUID databaseId = UUID.randomUUID();
        doNothing().when(schemaService).deleteDatabase(eq(databaseId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/schemas/databases/{id}", databaseId))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDatabase_ShouldReturnNotFound_WhenNotExists() throws Exception {
        UUID databaseId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Database not found"))
                .when(schemaService).deleteDatabase(eq(databaseId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/schemas/databases/{id}", databaseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Database not found"));
    }

    @Test
    void getTables_ShouldReturnPage() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        TableDto tableDto = new TableDto(tableId, "Users_Table", "Desc", now);
        Page<TableDto> page = new PageImpl<>(Collections.singletonList(tableDto), PageRequest.of(0, 10), 1);

        when(schemaService.getTables(eq(databaseId), any(CustomUserDetails.class), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/schemas/databases/{databaseId}/tables", databaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tableId").value(tableId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Users_Table"))
                .andExpect(jsonPath("$.content[0].description").value("Desc"))
                .andExpect(jsonPath("$.content[0].createdAt").value(now.format(formatter)));
    }

    @Test
    void getTable_ShouldReturnTable() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        TableDto tableDto = new TableDto(tableId, "My_Table", "Desc", now);

        when(schemaService.getTable(eq(databaseId), eq(tableId), any(CustomUserDetails.class)))
                .thenReturn(tableDto);

        mockMvc.perform(get("/api/v1/schemas/databases/{databaseId}/tables/{tableId}", databaseId, tableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableId").value(tableId.toString()))
                .andExpect(jsonPath("$.name").value("My_Table"))
                .andExpect(jsonPath("$.description").value("Desc"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void createTable_ShouldReturnCreated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        TableDto requestDto = new TableDto(null, "New_Table", "New Desc", null);
        TableDto responseDto = new TableDto(tableId, "New_Table", "New Desc", now);

        when(schemaService.createTable(eq(databaseId), any(TableDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/schemas/databases/{databaseId}/tables", databaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableId").value(tableId.toString()))
                .andExpect(jsonPath("$.name").value("New_Table"))
                .andExpect(jsonPath("$.description").value("New Desc"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void createTable_ShouldReturnConflict_WhenNameExists() throws Exception {
        UUID databaseId = UUID.randomUUID();
        TableDto requestDto = new TableDto(null, "Existing_Table", "Desc", null);

        when(schemaService.createTable(eq(databaseId), any(TableDto.class), any(CustomUserDetails.class)))
                .thenThrow(new DuplicatedResourceException("There is already a Table with the name 'Existing_Table'"));

        mockMvc.perform(post("/api/v1/schemas/databases/{databaseId}/tables", databaseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is already a Table with the name 'Existing_Table'"));
    }

    @Test
    void updateTable_ShouldReturnUpdated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        TableDto updateDto = new TableDto(tableId, "Updated_Table", "Updated Desc", null);
        TableDto responseDto = new TableDto(tableId, "Updated_Table", "Updated Desc", now);

        when(schemaService.updateTable(eq(databaseId), eq(tableId), any(TableDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/schemas/databases/{databaseId}/tables/{tableId}", databaseId, tableId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableId").value(tableId.toString()))
                .andExpect(jsonPath("$.name").value("Updated_Table"))
                .andExpect(jsonPath("$.description").value("Updated Desc"))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void deleteTable_ShouldReturnNotFound_WhenNotExists() throws Exception {
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Table not found"))
                .when(schemaService).deleteTable(eq(databaseId), eq(tableId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/schemas/databases/{databaseId}/tables/{tableId}", databaseId, tableId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Table not found"));
    }

    @Test
    void getColumns_ShouldReturnPage() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        ColumnDto columnDto = new ColumnDto(columnId, "username", "VARCHAR", false, now);
        Page<ColumnDto> page = new PageImpl<>(Collections.singletonList(columnDto), PageRequest.of(0, 10), 1);

        when(schemaService.getColumns(eq(databaseId), eq(tableId), any(CustomUserDetails.class), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/schemas/databases/{databaseId}/tables/{tableId}/columns", databaseId, tableId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].columnId").value(columnId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("username"))
                .andExpect(jsonPath("$.content[0].dataType").value("VARCHAR"))
                .andExpect(jsonPath("$.content[0].primaryKey").value(Boolean.FALSE.toString().toLowerCase()))
                .andExpect(jsonPath("$.content[0].createdAt").value(now.format(formatter)));
    }

    @Test
    void getColumn_ShouldReturnColumn() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        ColumnDto columnDto = new ColumnDto(columnId, "email", "VARCHAR", false, now);

        when(schemaService.getColumn(eq(databaseId), eq(tableId), eq(columnId), any(CustomUserDetails.class)))
                .thenReturn(columnDto);

        mockMvc.perform(get("/api/v1/schemas/databases/{databaseId}/tables/{tableId}/columns/{columnId}", databaseId, tableId, columnId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(columnId.toString()))
                .andExpect(jsonPath("$.name").value("email"))
                .andExpect(jsonPath("$.dataType").value("VARCHAR"))
                .andExpect(jsonPath("$.primaryKey").value(Boolean.FALSE.toString().toLowerCase()))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void createColumn_ShouldReturnCreated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        ColumnDto requestDto = new ColumnDto(null, "user_id", "UUID", true, null);
        ColumnDto responseDto = new ColumnDto(columnId, "user_id", "UUID", true, now);

        when(schemaService.createColumn(eq(databaseId), eq(tableId), any(ColumnDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/schemas/databases/{databaseId}/tables/{tableId}/columns", databaseId, tableId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.columnId").value(columnId.toString()))
                .andExpect(jsonPath("$.name").value("user_id"))
                .andExpect(jsonPath("$.dataType").value("UUID"))
                .andExpect(jsonPath("$.primaryKey").value(Boolean.TRUE.toString().toLowerCase()))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void createColumn_ShouldReturnConflict_WhenNameExists() throws Exception {
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();

        ColumnDto requestDto = new ColumnDto(null, "existing_col", "VARCHAR", false, null);

        when(schemaService.createColumn(eq(databaseId), eq(tableId), any(ColumnDto.class), any(CustomUserDetails.class)))
                .thenThrow(new DuplicatedResourceException("There is already a Column with the name 'existing_col'"));

        mockMvc.perform(post("/api/v1/schemas/databases/{databaseId}/tables/{tableId}/columns", databaseId, tableId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("There is already a Column with the name 'existing_col'"));
    }

    @Test
    void updateColumn_ShouldReturnUpdated() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        ColumnDto updateDto = new ColumnDto(columnId, "new_col", "INT", false, null);
        ColumnDto responseDto = new ColumnDto(columnId, "new_col", "INT", false, now);

        when(schemaService.updateColumn(eq(databaseId), eq(tableId), eq(columnId), any(ColumnDto.class), any(CustomUserDetails.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/schemas/databases/{databaseId}/tables/{tableId}/columns/{columnId}", databaseId, tableId, columnId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(columnId.toString()))
                .andExpect(jsonPath("$.name").value("new_col"))
                .andExpect(jsonPath("$.dataType").value("INT"))
                .andExpect(jsonPath("$.primaryKey").value(Boolean.FALSE.toString().toLowerCase()))
                .andExpect(jsonPath("$.createdAt").value(now.format(formatter)));
    }

    @Test
    void deleteColumn_ShouldReturnNotFound_WhenNotExists() throws Exception {
        UUID databaseId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();

        doThrow(new ResourceNotFoundException("Column not found"))
                .when(schemaService).deleteColumn(eq(databaseId), eq(tableId), eq(columnId), any(CustomUserDetails.class));

        mockMvc.perform(delete("/api/v1/schemas/databases/{databaseId}/tables/{tableId}/columns/{columnId}", databaseId, tableId, columnId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Column not found"));
    }
}