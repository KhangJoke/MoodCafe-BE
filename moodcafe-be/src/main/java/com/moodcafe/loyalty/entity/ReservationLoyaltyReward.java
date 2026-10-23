package com.moodcafe.loyalty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "reservation_loyalty_rewards",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_reservation_reward_type", columnNames = {"reservation_id", "reward_type"}),
                @UniqueConstraint(name = "uq_booking_loyalty_reward", columnNames = {"booking_id", "reward_type"})
        },
        indexes = {
                @Index(name = "idx_loyalty_rewards_user", columnList = "user_id"),
                @Index(name = "idx_loyalty_rewards_booking", columnList = "booking_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationLoyaltyReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reward_id", nullable = false, updatable = false)
    private UUID rewardId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * SPEND, ON_TIME_CHECK_IN, SELF_CHECK_OUT, VERIFIED_REVIEW
     */
    @Column(name = "reward_type", nullable = false, length = 50)
    private String rewardType;

    @Column(name = "points_awarded", nullable = false)
    private Long pointsAwarded;

    @Column(name = "lot_id")
    private UUID lotId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
