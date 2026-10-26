package com.moodcafe.booking.entity;

import com.moodcafe.store.entity.Store;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        name = "store_booking_configs",
        indexes = {
                @Index(name = "idx_store_booking_configs_store_id", columnList = "store_id")
        }
)
@SQLDelete(sql = "UPDATE store_booking_configs SET is_deleted = true WHERE config_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreBookingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id", nullable = false, updatable = false)
    private UUID configId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Builder.Default
    @Column(name = "is_booking_enabled", nullable = false)
    private Boolean isBookingEnabled = false;

    @Builder.Default
    @Column(name = "is_time_limit_enabled", nullable = false)
    private Boolean isTimeLimitEnabled = true;

    @Builder.Default
    @Column(name = "sitting_duration_mins", nullable = false)
    private Integer sittingDurationMins = 120;

    @Builder.Default
    @Column(name = "cleanup_buffer_mins", nullable = false)
    private Integer cleanupBufferMins = 15;

    @Builder.Default
    @Column(name = "grace_period_mins", nullable = false)
    private Integer gracePeriodMins = 30;

    @Builder.Default
    @Column(name = "min_advance_mins", nullable = false)
    private Integer minAdvanceMins = 30;

    @Builder.Default
    @Column(name = "free_cancel_hours", nullable = false)
    private Integer freeCancelHours = 2;

    @Builder.Default
    @Column(name = "is_early_check_in_enabled", nullable = false)
    private Boolean isEarlyCheckInEnabled = true;

    @Builder.Default
    @Column(name = "max_early_check_in_mins", nullable = false)
    private Integer maxEarlyCheckInMins = 60;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}
