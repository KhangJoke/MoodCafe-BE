package com.moodcafe.gamification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "quest_audit_logs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quest_audit_source", columnNames = {"quest_id", "user_id", "source_ref_id"})
        },
        indexes = {
                @Index(name = "idx_qal_antifarm", columnList = "user_id, store_id, action_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", nullable = false, updatable = false)
    private UUID auditId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "quest_id", nullable = false)
    private UUID questId;

    @Column(name = "period_key", nullable = false, length = 30)
    private String periodKey;

    @Column(name = "source_ref_id", nullable = false, length = 100)
    private String sourceRefId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "action_date", nullable = false)
    private LocalDate actionDate;

    @Column(name = "value_added", nullable = false)
    @Builder.Default
    private Long valueAdded = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
