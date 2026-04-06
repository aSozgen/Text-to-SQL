package com.texttosql.backend.unit;

import com.texttosql.backend.client.LlmClient;
import com.texttosql.backend.dto.llm.ConversationTurn;
import com.texttosql.backend.dto.llm.LLMMetadata;
import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class LlmClientTest {

    private LlmClient llmClient;

    @BeforeEach
    void setUp() {
        llmClient = new LlmClient("http://localhost:8000", "/api/predict", 5000, 5000);
    }

    private LLMResponse createMockResponse(String status, String sql, boolean isValid, Double confidence, boolean schemaUsed, boolean contextUsed, Integer contextTurns, Long processingTime) {
        LLMMetadata metadata = new LLMMetadata(
                "v1.0",
                "2026-03-12T10:00:00Z",
                "GPU",
                5,
                150,
                10
        );

        return new LLMResponse(
                status,
                sql,
                isValid,
                null,
                confidence,
                schemaUsed,
                contextUsed,
                contextTurns,
                processingTime,
                null,
                metadata
        );
    }

    @Test
    void generateSql_ShouldReturnResponse_WhenRequestIsSuccessful() {
        String question = "Find all users";
        // GÜNCELLEME: String yerine Map<String, Object> formatında JSON şema
        Map<String, Object> schema = Map.of("tables", List.of(
                Map.of("tableName", "users", "columns", List.of(
                        Map.of("columnName", "id", "dataType", "integer"),
                        Map.of("columnName", "name", "dataType", "varchar")
                ))
        ));

        LLMRequest request = new LLMRequest(question, schema);

        LLMResponse expectedResponse = createMockResponse(
                "success",
                "SELECT * FROM users",
                true,
                0.95,
                true,
                false,
                0,
                250L
        );

        assertThat(expectedResponse.sql()).isEqualTo("SELECT * FROM users");
        assertThat(expectedResponse.confidence()).isEqualTo(0.95);
        assertThat(expectedResponse.status()).isEqualTo("success");
        assertThat(expectedResponse.isValid()).isTrue();
        assertThat(request.schema()).containsKey("tables");
    }

    @Test
    void generateSql_ShouldHandleMultipleRequests() {
        LLMResponse response1 = createMockResponse("success", "SELECT * FROM users", true, 0.92, false, false, 0, 200L);
        LLMResponse response2 = createMockResponse("success", "SELECT * FROM orders", true, 0.88, false, false, 0, 180L);

        assertThat(response1.sql()).isNotEqualTo(response2.sql());
        assertThat(response1.confidence()).isGreaterThan(response2.confidence());
    }

    @Test
    void generateSql_ShouldHandleValidationError() {
        String question = "Find users";
        LLMRequest request = new LLMRequest(question);

        LLMResponse expectedResponse = new LLMResponse(
                "error",
                null,
                false,
                "Invalid SQL syntax",
                0.45,
                false,
                false,
                0,
                180L,
                "SQL validation failed",
                new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "GPU", 5, 100, 10)
        );

        assertThat(expectedResponse.isValid()).isFalse();
        assertThat(expectedResponse.validationError()).isEqualTo("Invalid SQL syntax");
        assertThat(expectedResponse.status()).isEqualTo("error");
        assertThat(expectedResponse.error()).isEqualTo("SQL validation failed");
    }

    @Test
    void generateSql_ShouldHandleComplexQueries() {
        String complexQuestion = "Find users with orders from last month";
        Map<String, Object> schema = Map.of("tables", List.of(
                Map.of("tableName", "users"),
                Map.of("tableName", "orders")
        ));

        LLMRequest request = new LLMRequest(complexQuestion, schema);

        LLMResponse expectedResponse = new LLMResponse(
                "success",
                "SELECT u.* FROM users u JOIN orders o ON u.id = o.user_id WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH)",
                true,
                null,
                0.87,
                true,
                false,
                0,
                350L,
                null,
                new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "GPU", 5, 250, 10)
        );

        assertThat(expectedResponse).isNotNull();
        assertThat(expectedResponse.confidence()).isLessThan(0.95);
        assertThat(expectedResponse.sql()).contains("JOIN");
        assertThat(expectedResponse.schemaUsed()).isTrue();
    }

    @Test
    void generateSql_ShouldHandleConversationHistory() {
        List<ConversationTurn> history = Arrays.asList(
                new ConversationTurn("Find all users", "SELECT * FROM users"),
                new ConversationTurn("Filter by age > 18", "SELECT * FROM users WHERE age > 18")
        );
        LLMRequest request = new LLMRequest("Sort by name", history);

        LLMResponse expectedResponse = new LLMResponse(
                "success",
                "SELECT * FROM users WHERE age > 18 ORDER BY name",
                true,
                null,
                0.91,
                true,
                true,
                2,
                400L,
                null,
                new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "GPU", 5, 300, 10)
        );

        assertThat(expectedResponse.contextUsed()).isTrue();
        assertThat(expectedResponse.contextTurns()).isEqualTo(2);
        assertThat(request.conversationHistory()).hasSize(2);
    }

    @Test
    void generateSql_ShouldIncludeMetadata() {
        String question = "Find users";
        LLMRequest request = new LLMRequest(question);

        LLMMetadata metadata = new LLMMetadata(
                "v2.0",
                "2026-03-12T10:15:30Z",
                "GPU",
                8,
                120,
                15
        );

        LLMResponse expectedResponse = new LLMResponse(
                "success",
                "SELECT * FROM users",
                true,
                null,
                0.93,
                false,
                false,
                0,
                200L,
                null,
                metadata
        );

        assertThat(expectedResponse.metadata()).isNotNull();
        assertThat(expectedResponse.metadata().modelVersion()).isEqualTo("v2.0");
        assertThat(expectedResponse.metadata().device()).isEqualTo("GPU");
        assertThat(expectedResponse.metadata().numBeams()).isEqualTo(8);
    }

    @Test
    void generateSql_ShouldHandleNullSchema() {
        String question = "Find users";
        LLMRequest request = new LLMRequest(question);

        assertThat(request.schema()).isNull();

        LLMResponse expectedResponse = createMockResponse(
                "success",
                "SELECT * FROM users",
                true,
                0.85,
                false,
                false,
                0,
                180L
        );

        assertThat(expectedResponse).isNotNull();
        assertThat(expectedResponse.schemaUsed()).isFalse();
    }

    @Test
    void llmRequest_ShouldSupportMultipleConstructors() {
        LLMRequest request1 = new LLMRequest("Find users");
        assertThat(request1.question()).isEqualTo("Find users");
        assertThat(request1.schema()).isNull();
        assertThat(request1.conversationHistory()).isNull();

        Map<String, Object> schemaMap = Map.of("tables", Collections.emptyList());
        LLMRequest request2 = new LLMRequest("Find users", schemaMap);
        assertThat(request2.question()).isEqualTo("Find users");
        assertThat(request2.schema()).isNotNull();
        assertThat(request2.schema()).isInstanceOf(Map.class);
        assertThat(request2.conversationHistory()).isNull();

        List<ConversationTurn> history = Arrays.asList(
                new ConversationTurn("Q1", "SQL1"),
                new ConversationTurn("Q2", "SQL2")
        );
        LLMRequest request3 = new LLMRequest("Find users", history);
        assertThat(request3.question()).isEqualTo("Find users");
        assertThat(request3.schema()).isNull();
        assertThat(request3.conversationHistory()).hasSize(2);
    }

    @Test
    void generateSql_EdgeCase_ShouldHandleEmptySchemaMap() {
        Map<String, Object> emptySchema = Collections.emptyMap();
        LLMRequest request = new LLMRequest("Select everything", emptySchema);

        assertThat(request.schema()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateSql_EdgeCase_ShouldHandleDeeplyNestedSchemaMap() {
        Map<String, Object> deepSchema = Map.of(
                "tables", List.of(
                        Map.of(
                                "tableName", "departments",
                                "metadata", Map.of("owner", "admin", "tags", List.of("hr", "finance")),
                                "columns", List.of(Map.of("columnName", "id", "dataType", "uuid"))
                        )
                )
        );

        LLMRequest request = new LLMRequest("Find departments", deepSchema);

        assertThat(request.schema()).containsKey("tables");

        List<Map<String, Object>> tables = (List<Map<String, Object>>) request.schema().get("tables");
        assertThat(tables).hasSize(1);

        Map<String, Object> firstTable = tables.get(0);
        assertThat(firstTable).containsKey("metadata");
    }

    @Test
    void generateSql_EdgeCase_ShouldHandleNullQuestion() {
        LLMRequest request = new LLMRequest(null, Map.of());

        assertThat(request.question()).isNull();
    }

    @Test
    void generateSql_EdgeCase_ShouldHandleExtremelyLongConversationHistory() {
        List<ConversationTurn> longHistory = Collections.nCopies(100,
                new ConversationTurn("Give me the count", "SELECT COUNT(*) FROM table")
        );

        LLMRequest request = new LLMRequest("And now group by date", longHistory);

        assertThat(request.conversationHistory()).hasSize(100);
        assertThat(request.conversationHistory().get(99).question()).isEqualTo("Give me the count");
    }
}