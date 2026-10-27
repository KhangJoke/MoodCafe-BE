package com.moodcafe.gamification.entity;

import com.moodcafe.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_titles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_titles_user_title", columnNames = {"user_id", "title_id"})
        },
        indexes = {
                @Index(name = "idx_user_titles_user_id", columnList = "user_id"),
                @Index(name = "idx_user_titles_equipped", columnList = "user_id, is_equipped")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_title_id", nullable = false, updatable = false)
    private UUID userTitleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "title_id", nullable = false)
    private Title title;

    @Column(name = "is_equipped", nullable = false)
    @Builder.Default
    private boolean equipped = false;

    @CreationTimestamp
    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}
