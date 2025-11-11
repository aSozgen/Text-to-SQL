package com.texttosql.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tables")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "table_id", columnDefinition = "UUID")
    private UUID tableId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_id", nullable = false)
    private DatabaseEntity databaseId;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "tableId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnEntity> columns = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
