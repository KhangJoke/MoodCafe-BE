package com.moodcafe.tag.entity;

import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tag_categories",
        indexes = {
                @Index(name = "idx_tag_categories_code", columnList = "code"),
                @Index(name = "idx_tag_categories_active", columnList = "is_active"),
                @Index(name = "idx_tag_categories_is_deleted", columnList = "is_deleted")
        }
)
@SQLDelete(sql = "UPDATE tag_categories SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE tag_category_id = ?")
@SQLRestriction("is_deleted = false")
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

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
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

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
