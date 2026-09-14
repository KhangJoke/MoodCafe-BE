package com.moodcafe.store.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "store_roles",
        indexes = {
                @Index(name = "idx_store_roles_name", columnList = "name", unique = true)
        }
)
@SQLDelete(sql = "UPDATE store_roles SET is_deleted = true WHERE store_role_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "store_role_id", nullable = false, updatable = false)
    private UUID storeRoleId;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
