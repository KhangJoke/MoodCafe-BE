package com.moodcafe.gamification.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.gamification.entity.enums.RuleType;
import com.moodcafe.voucher.entity.VoucherTemplate;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "quests",
        indexes = {
                @Index(name = "idx_quests_code", columnList = "code", unique = true),
                @Index(name = "idx_quests_group", columnList = "quest_group"),
                @Index(name = "idx_quests_type", columnList = "quest_type")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quest_id", nullable = false, updatable = false)
    private UUID questId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_group", nullable = false, length = 30)
    private QuestGroup questGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_type", nullable = false, length = 20)
    private QuestType questType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Column(name = "target_value", nullable = false)
    @Builder.Default
    private Long targetValue = 1L;

    @Column(name = "reward_points", nullable = false)
    @Builder.Default
    private Long rewardPoints = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_voucher_template_id")
    private VoucherTemplate rewardVoucherTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_title_id")
    private Title rewardTitle;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean system = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
