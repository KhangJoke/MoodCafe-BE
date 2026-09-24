package com.moodcafe.tag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_preferences",
        indexes = {
                @Index(name = "idx_user_preferences_user_id", columnList = "user_id"),
                @Index(name = "idx_user_preferences_question_id", columnList = "question_id"),
                @Index(name = "idx_user_preferences_is_deleted", columnList = "is_deleted")
        }
)
@SQLDelete(sql = "UPDATE user_preferences SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE preference_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "preference_id", nullable = false, updatable = false)
    private UUID preferenceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private OnboardingQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id")
    private Tag tag;

    @Column(name = "numeric_value")
    private Integer numericValue;

    @Builder.Default
    @Column(name = "is_skipped", nullable = false)
    private boolean skipped = false;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
