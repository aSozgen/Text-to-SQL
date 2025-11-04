package com.texttosql.backend.entity;

import com.texttosql.backend.util.FeedbackEnum;
import com.texttosql.backend.util.SenderTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id", columnDefinition = "UUID")
    private UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatEntity chatId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> schema;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private SenderTypeEnum senderType;

    @Column(nullable = false)
    @Builder.Default
    private FeedbackEnum feedback = FeedbackEnum.NONE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
