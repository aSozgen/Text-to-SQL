package com.texttosql.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private List<Map<String, String>> buildRows(ChatEntity chat) {
        List<MessageEntity> messages =
                messageRepository.findByChatAndActiveTrueOrderByCreatedAtAsc(chat);

        return messages.stream().map(msg -> {
            boolean isLlm = msg.getSenderType() == SenderType.LLM;
            Map<String, String> row = new LinkedHashMap<>();
            row.put("timestamp",  msg.getCreatedAt().format(DATE_FORMATTER));
            row.put("sender",     isLlm ? "LLM" : "User");
            row.put("content",    msg.getContent());
            row.put("confidence", isLlm && msg.getConfidence() != null
                    ? String.format(java.util.Locale.US, "%.2f%%", msg.getConfidence()) : "");
            row.put("feedback",   isLlm && msg.getFeedback() != null
                    ? String.valueOf(msg.getFeedback()) : "");
            return row;
        }).toList();
    }

    @Transactional(readOnly = true)
    public String exportChatToCsv(ChatEntity chat) {
        try {
            List<Map<String, String>> rows = buildRows(chat);
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);

            csvWriter.writeNext(new String[]{"Timestamp", "Sender", "Content", "Confidence", "Feedback"});

            for (Map<String, String> row : rows) {
                csvWriter.writeNext(new String[]{
                        row.get("timestamp"),
                        row.get("sender"),
                        row.get("content"),
                        row.get("confidence"),
                        row.get("feedback")
                });
            }

            csvWriter.close();
            log.info("Exported chat {} to CSV – {} messages", chat.getChatId(), rows.size());
            return stringWriter.toString();

        } catch (Exception e) {
            log.error("Error exporting chat to CSV", e);
            throw new RuntimeException("Failed to export chat", e);
        }
    }

    @Transactional(readOnly = true)
    public String exportChatToMarkdown(ChatEntity chat) {
        List<Map<String, String>> rows = buildRows(chat);
        StringBuilder md = new StringBuilder();

        md.append("# ").append(chat.getName()).append("\n\n");
        md.append("**Created:** ").append(chat.getCreatedAt().format(DATE_FORMATTER)).append("\n\n");
        md.append("---\n\n");

        for (Map<String, String> row : rows) {
            boolean isLlm = "LLM".equals(row.get("sender"));

            if (isLlm) {
                md.append("## 🤖 AI Assistant");
                if (!row.get("confidence").isEmpty())
                    md.append(" (Confidence: ").append(row.get("confidence")).append(")");
                md.append("\n\n```sql\n").append(row.get("content")).append("\n```\n\n");
                if (!row.get("feedback").isEmpty())
                    md.append("**Feedback:** ").append(row.get("feedback")).append("\n\n");
            } else {
                md.append("## 👤 User\n\n");
                md.append(row.get("content")).append("\n\n");
            }

            md.append("*").append(row.get("timestamp")).append("*\n\n---\n\n");
        }

        log.info("Exported chat {} to Markdown – {} messages", chat.getChatId(), rows.size());
        return md.toString();
    }

    @Transactional(readOnly = true)
    public String exportChatToJson(ChatEntity chat) {
        try {
            List<Map<String, String>> rows = buildRows(chat);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("chatId",    chat.getChatId());
            payload.put("title",     chat.getName());
            payload.put("createdAt", chat.getCreatedAt().format(DATE_FORMATTER));
            payload.put("messages",  rows);

            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);

            return mapper.writeValueAsString(payload);

        } catch (Exception e) {
            throw new RuntimeException("Failed to export chat", e);
        }
    }
}