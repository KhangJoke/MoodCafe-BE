package com.moodcafe.tag.entity;

import com.moodcafe.tag.entity.enums.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "onboarding_questions",
        indexes = {
                @Index(name = "idx_onboarding_questions_active", columnList = "is_active"),
                @Index(name = "idx_onboarding_questions_order", columnList = "display_order"),
                @Index(name = "idx_onboarding_questions_is_deleted", columnList = "is_deleted")
        }
)
@SQLDelete(sql = "UPDATE onboarding_questions SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE question_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_category_id")
    private TagCategory tagCategory;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "subtitle", columnDefinition = "TEXT")
    private String subtitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @Builder.Default
    @Column(name = "is_required", nullable = false)
    private boolean required = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "max_selections")
    private Integer maxSelections = 3;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
