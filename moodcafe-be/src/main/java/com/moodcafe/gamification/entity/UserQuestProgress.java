package com.moodcafe.gamification.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.gamification.entity.enums.QuestProgressStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_quest_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_quest_progress_period", columnNames = {"user_id", "quest_id", "period_key"})
        },
        indexes = {
                @Index(name = "idx_uqp_user_period", columnList = "user_id, period_key"),
                @Index(name = "idx_uqp_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuestProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_quest_id", nullable = false, updatable = false)
    private UUID userQuestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @Column(name = "period_key", nullable = false, length = 30)
    private String periodKey;

    @Column(name = "current_value", nullable = false)
    @Builder.Default
    private Long currentValue = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private QuestProgressStatus status = QuestProgressStatus.IN_PROGRESS;

    @Column(name = "claimable_at")
    private Instant claimableAt;

    @Column(name = "rewarded_at")
    private Instant rewardedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
