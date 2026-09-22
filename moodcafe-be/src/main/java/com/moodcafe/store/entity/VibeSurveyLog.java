package com.moodcafe.store.entity;

import com.moodcafe.store.entity.enums.VibeSurveyResponse;
import com.moodcafe.tag.entity.Tag;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vibe_survey_logs",
        indexes = {
                @Index(name = "idx_vibe_survey_logs_visit_id", columnList = "visit_verification_id"),
                @Index(name = "idx_vibe_survey_logs_tag_id", columnList = "tag_id"),
                @Index(name = "idx_vibe_survey_logs_response", columnList = "response")
        }
)
@SQLDelete(sql = "UPDATE vibe_survey_logs SET is_deleted = true WHERE vibe_survey_log_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VibeSurveyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "vibe_survey_log_id", nullable = false, updatable = false)
    private UUID vibeSurveyLogId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_verification_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vibe_survey_visit"))
    private VisitVerification visitVerification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vibe_survey_tag"))
    private Tag tag;

    @Enumerated(EnumType.STRING)
    @Column(name = "response", nullable = false, length = 30)
    private VibeSurveyResponse response;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
