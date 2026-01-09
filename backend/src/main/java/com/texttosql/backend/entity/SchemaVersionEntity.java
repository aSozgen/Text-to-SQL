package com.texttosql.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schema_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "version_id", columnDefinition = "UUID")
    private UUID versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false, updatable = false)
    private DatabaseEntity database;

    @Column(name = "version_number", nullable = false, updatable = false)
    @Builder.Default
    private int versionNumber = 0;

    @Lob
    @Column(name = "schema_structure", nullable = false, columnDefinition = "TEXT")
    private String schemaStructure;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
