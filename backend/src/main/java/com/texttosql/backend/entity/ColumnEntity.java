package com.texttosql.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "columns")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "column_id", columnDefinition = "UUID")
    private UUID columnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableEntity table;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_type")
    private String dataType;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
