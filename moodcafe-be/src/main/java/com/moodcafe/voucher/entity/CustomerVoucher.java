package com.moodcafe.voucher.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_vouchers", indexes = {
        @Index(name = "idx_customer_vouchers_user", columnList = "user_id, status"),
        @Index(name = "idx_customer_vouchers_lock", columnList = "locked_reservation_id, status"),
        @Index(name = "idx_customer_vouchers_booking_lock", columnList = "locked_booking_id, status"),
        @Index(name = "idx_customer_vouchers_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "voucher_id", nullable = false, updatable = false)
    private UUID voucherId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "face_value_vnd", nullable = false, precision = 12, scale = 2)
    private BigDecimal faceValueVnd;

    @Column(name = "point_cost", nullable = false)
    private Long pointCost;

    /**
     * AVAILABLE, LOCKED, USED, EXPIRED, REVOKED
     */
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "AVAILABLE";

    @Column(name = "locked_reservation_id")
    private UUID lockedReservationId;

    @Column(name = "locked_booking_id")
    private UUID lockedBookingId;

    @Column(name = "hold_until")
    private Instant holdUntil;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "min_gross_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minGrossAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
