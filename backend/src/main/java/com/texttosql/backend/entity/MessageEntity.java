package com.texttosql.backend.entity;

import com.texttosql.backend.util.Feedback;
import com.texttosql.backend.util.SenderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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
    @JoinColumn(name = "chat_id", nullable = false, updatable = false)
    private ChatEntity chat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schema_version_id", updatable = false)
    private SchemaVersionEntity schemaVersion;

    @Column(nullable = false, updatable = false)
    private String content;

    private Double confidence;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private SenderType senderType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Feedback feedback = Feedback.NONE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
