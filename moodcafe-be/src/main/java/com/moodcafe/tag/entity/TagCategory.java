package com.moodcafe.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tag_categories",
        indexes = {
                @Index(name = "idx_tag_categories_code", columnList = "code", unique = true),
                @Index(name = "idx_tag_categories_active", columnList = "is_active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tag_category_id", nullable = false, updatable = false)
    private UUID tagCategoryId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_mode", nullable = false, length = 30)
    private ApprovalMode approvalMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 30)
    private ControlType controlType;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
