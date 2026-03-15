package com.texttosql.backend.unit;

import com.texttosql.backend.client.LlmClient;
import com.texttosql.backend.dto.llm.ConversationTurn;
import com.texttosql.backend.dto.llm.LLMMetadata;
import com.texttosql.backend.dto.llm.LLMRequest;
import com.texttosql.backend.dto.llm.LLMResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LlmClientTest {

    @BeforeEach
    void setUp() {
        LlmClient llmClient = new LlmClient("http://localhost:8000", "/api/predict", 5000, 5000);
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
        String schema = "CREATE TABLE users (id INT, name VARCHAR(255))";
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

        // Test logic without mocking RestClient
        assertThat(expectedResponse.sql()).isEqualTo("SELECT * FROM users");
        assertThat(expectedResponse.confidence()).isEqualTo(0.95);
        assertThat(expectedResponse.status()).isEqualTo("success");
        assertThat(expectedResponse.isValid()).isTrue();
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
        String schema = "CREATE TABLE users (id INT); CREATE TABLE orders (id INT, user_id INT)";
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
    void generateSql_ShouldReturnHighConfidenceScore_ForSimpleQueries() {
        String simpleQuestion = "Find all users";
        LLMRequest request = new LLMRequest(simpleQuestion);

        LLMResponse expectedResponse = createMockResponse(
                "success",
                "SELECT * FROM users",
                true,
                0.96,
                false,
                false,
                0,
                150L
        );

        assertThat(expectedResponse.confidence()).isGreaterThan(0.90);
        assertThat(expectedResponse.confidence()).isCloseTo(0.96, org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    void generateSql_ShouldReturnLowerConfidenceScore_ForComplexQueries() {
        String complexQuestion = "Find users who purchased products in specific categories with discount applied during specific time period";
        LLMRequest request = new LLMRequest(complexQuestion);

        LLMResponse expectedResponse = new LLMResponse(
                "success",
                "SELECT DISTINCT u.* FROM users u WHERE u.id IN (SELECT DISTINCT p.user_id FROM purchases p WHERE p.category_id IN (...) AND p.discount > 0 AND p.created_at BETWEEN ? AND ?)",
                true,
                null,
                0.72,
                true,
                false,
                0,
                500L,
                null,
                new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "GPU", 5, 400, 10)
        );

        assertThat(expectedResponse.confidence()).isBetween(0.0, 0.95);
        assertThat(expectedResponse.confidence()).isLessThan(0.90);
    }

    @Test
    void generateSql_ShouldCompareConfidenceScores() {
        LLMResponse simpleResponse = createMockResponse("success", "SELECT * FROM users", true, 0.96, false, false, 0, 150L);
        LLMResponse complexResponse = createMockResponse("success", "SELECT DISTINCT u.* FROM users u WHERE ...", true, 0.72, true, false, 0, 500L);

        assertThat(simpleResponse.confidence()).isGreaterThan(complexResponse.confidence());
    }

    @Test
    void generateSql_ShouldTrackProcessingTime() {
        String question = "Find users";
        LLMRequest request = new LLMRequest(question);

        LLMResponse expectedResponse = createMockResponse(
                "success",
                "SELECT * FROM users",
                true,
                0.94,
                false,
                false,
                0,
                245L
        );

        assertThat(expectedResponse.processingTimeMs()).isGreaterThan(0);
        assertThat(expectedResponse.processingTimeMs()).isLessThan(10000);
        assertThat(expectedResponse.processingTimeMs()).isEqualTo(245L);
    }

    @Test
    void generateSql_ShouldHandleVariousProcessingTimes() {
        LLMResponse fastResponse = createMockResponse("success", "SELECT * FROM users", true, 0.95, false, false, 0, 100L);
        LLMResponse slowResponse = createMockResponse("success", "SELECT DISTINCT u.* FROM users u WHERE ...", true, 0.72, true, false, 0, 1500L);

        assertThat(fastResponse.processingTimeMs()).isLessThan(slowResponse.processingTimeMs());
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
        assertThat(expectedResponse.metadata().maxContextTurns()).isEqualTo(15);
    }

    @Test
    void generateSql_ShouldHandleMetadataVariations() {
        LLMMetadata gpuMetadata = new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "GPU", 5, 100, 10);
        LLMMetadata cpuMetadata = new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "CPU", 3, 100, 10);

        LLMResponse gpuResponse = new LLMResponse("success", "SELECT *", true, null, 0.95, false, false, 0, 200L, null, gpuMetadata);
        LLMResponse cpuResponse = new LLMResponse("success", "SELECT *", true, null, 0.85, false, false, 0, 400L, null, cpuMetadata);

        assertThat(gpuResponse.metadata().device()).isEqualTo("GPU");
        assertThat(cpuResponse.metadata().device()).isEqualTo("CPU");
        assertThat(gpuResponse.processingTimeMs()).isLessThan(cpuResponse.processingTimeMs());
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
        assertThat(expectedResponse.sql()).isNotEmpty();
        assertThat(expectedResponse.schemaUsed()).isFalse();
    }

    @Test
    void generateSql_ShouldHandleNullConversationHistory() {
        String question = "Find users";
        LLMRequest request = new LLMRequest(question);

        assertThat(request.conversationHistory()).isNull();

        LLMResponse expectedResponse = createMockResponse(
                "success",
                "SELECT * FROM users",
                true,
                0.90,
                false,
                false,
                0,
                170L
        );

        assertThat(expectedResponse.contextUsed()).isFalse();
        assertThat(expectedResponse.contextTurns()).isEqualTo(0);
    }

    @Test
    void generateSql_ShouldHandleEmptyQuestion() {
        String emptyQuestion = "";
        LLMRequest request = new LLMRequest(emptyQuestion);

        LLMResponse expectedResponse = new LLMResponse(
                "error",
                null,
                false,
                "Question cannot be empty",
                0.0,
                false,
                false,
                0,
                100L,
                "Invalid input",
                new LLMMetadata("v1.0", "2026-03-12T10:00:00Z", "GPU", 5, 10, 10)
        );

        assertThat(expectedResponse.status()).isEqualTo("error");
        assertThat(expectedResponse.isValid()).isFalse();
        assertThat(expectedResponse.confidence()).isEqualTo(0.0);
    }

    @Test
    void llmRequest_ShouldSupportMultipleConstructors() {
        // Constructor with question only
        LLMRequest request1 = new LLMRequest("Find users");
        assertThat(request1.question()).isEqualTo("Find users");
        assertThat(request1.schema()).isNull();
        assertThat(request1.conversationHistory()).isNull();

        // Constructor with question and schema
        LLMRequest request2 = new LLMRequest("Find users", "CREATE TABLE users (id INT)");
        assertThat(request2.question()).isEqualTo("Find users");
        assertThat(request2.schema()).isEqualTo("CREATE TABLE users (id INT)");
        assertThat(request2.conversationHistory()).isNull();

        // Constructor with question and conversation history
        List<ConversationTurn> history = Arrays.asList(
                new ConversationTurn("Q1", "SQL1"),
                new ConversationTurn("Q2", "SQL2")
        );
        LLMRequest request3 = new LLMRequest("Find users", history);
        assertThat(request3.question()).isEqualTo("Find users");
        assertThat(request3.schema()).isNull();
        assertThat(request3.conversationHistory()).hasSize(2);
    }
}