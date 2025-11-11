package com.texttosql.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "databases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "database_id", columnDefinition = "UUID")
    private UUID databaseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity userId;

    @Column(nullable = false)
    private String name;

    private String description;

    @OneToMany(mappedBy = "databaseId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TableEntity> tables = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
