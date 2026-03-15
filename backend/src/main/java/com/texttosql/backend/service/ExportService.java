package com.texttosql.backend.service;

import com.opencsv.CSVWriter;
import com.texttosql.backend.entity.ChatEntity;
import com.texttosql.backend.entity.MessageEntity;
import com.texttosql.backend.repository.MessageRepository;
import com.texttosql.backend.entity.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public String exportChatToCsv(ChatEntity chat) {
        try {
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);

            // Write header
            String[] header = {"Timestamp", "Sender", "Message", "Confidence", "Feedback"};
            csvWriter.writeNext(header);

            // Get all messages
            List<MessageEntity> messages = messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chat);

            // Write data
            for (MessageEntity message : messages) {
                String[] data = {
                        message.getCreatedAt().format(DATE_FORMATTER),
                        message.getSenderType() == SenderType.USER ? "User" : "LLM",
                        message.getContent(),
                        message.getConfidence() != null ? String.format("%.2f%%", message.getConfidence()) : "",
                        message.getFeedback() != null ? message.getFeedback().toString() : ""
                };
                csvWriter.writeNext(data);
            }

            csvWriter.close();
            log.info("Exported chat {} to CSV - {} messages", chat.getChatId(), messages.size());
            return stringWriter.toString();

        } catch (Exception e) {
            log.error("Error exporting chat to CSV", e);
            throw new RuntimeException("Failed to export chat", e);
        }
    }

    @Transactional(readOnly = true)
    public String exportChatToMarkdown(ChatEntity chat) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(chat.getName()).append("\n\n");
        markdown.append("**Created:** ").append(chat.getCreatedAt().format(DATE_FORMATTER)).append("\n\n");
        markdown.append("---\n\n");

        List<MessageEntity> messages = messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chat);

        for (MessageEntity message : messages) {
            if (message.getSenderType() == SenderType.USER) {
                markdown.append("## 👤 User\n\n");
                markdown.append(message.getContent()).append("\n\n");
            } else {
                markdown.append("## 🤖 AI Assistant");
                if (message.getConfidence() != null) {
                    markdown.append(" (Confidence: ").append(String.format("%.1f%%", message.getConfidence())).append(")");
                }
                markdown.append("\n\n");
                markdown.append("```sql\n");
                markdown.append(message.getContent()).append("\n");
                markdown.append("```\n\n");

                if (message.getFeedback() != null) {
                    markdown.append("**Feedback:** ").append(message.getFeedback()).append("\n\n");
                }
            }
            markdown.append("---\n\n");
        }

        log.info("Exported chat {} to Markdown - {} messages", chat.getChatId(), messages.size());
        return markdown.toString();
    }

    @Transactional(readOnly = true)
    public String exportChatToJson(ChatEntity chat) {
        List<MessageEntity> messages = messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chat);

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"chatId\": \"").append(chat.getChatId()).append("\",\n");
        json.append("  \"title\": \"").append(escapeJson(chat.getName())).append("\",\n");
        json.append("  \"createdAt\": \"").append(chat.getCreatedAt().toString()).append("\",\n");
        json.append("  \"messages\": [\n");

        for (int i = 0; i < messages.size(); i++) {
            MessageEntity message = messages.get(i);
            json.append("    {\n");
            json.append("      \"timestamp\": \"").append(message.getCreatedAt().toString()).append("\",\n");
            json.append("      \"sender\": \"").append(message.getSenderType()).append("\",\n");
            json.append("      \"content\": \"").append(escapeJson(message.getContent())).append("\",\n");
            if (message.getConfidence() != null) {
                json.append("      \"confidence\": ").append(message.getConfidence()).append(",\n");
            }
            if (message.getFeedback() != null) {
                json.append("      \"feedback\": \"").append(message.getFeedback()).append("\",\n");
            }
            json.append("      \"active\": ").append(message.getActive()).append("\n");
            json.append("    }");
            if (i < messages.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        log.info("Exported chat {} to JSON - {} messages", chat.getChatId(), messages.size());
        return json.toString();
    }

    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
