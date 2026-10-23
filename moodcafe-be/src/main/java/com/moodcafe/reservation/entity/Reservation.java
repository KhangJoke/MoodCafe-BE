package com.moodcafe.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_store", columnList = "store_id, status, booking_time"),
        @Index(name = "idx_reservations_user", columnList = "user_id, status"),
        @Index(name = "idx_reservations_payos_code", columnList = "payos_order_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "reservation_id", nullable = false, updatable = false)
    private UUID reservationId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "table_id")
    private UUID tableId;

    @Column(name = "booking_time", nullable = false)
    private Instant bookingTime;

    @Column(name = "guest_count", nullable = false)
    @Builder.Default
    private Integer guestCount = 2;

    /**
     * PENDING_PAYMENT, PAID_CONFIRMED, CHECKED_IN, COMPLETED, TABLE_EXPIRED, CLAIMED_LATE, EXPIRED_NO_SHOW, CANCELLED_REFUNDED, PAYMENT_FAILED, PAYMENT_EXPIRED
     */
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private String status = "PENDING_PAYMENT";

    // 5 Financial Fields
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "voucher_discount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    @Column(name = "customer_paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal customerPaidAmount;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "merchant_net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal merchantNetAmount;

    // Voucher & Funding snapshot
    @Column(name = "voucher_id")
    private UUID voucherId;

    @Column(name = "voucher_refund_funded_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal voucherRefundFundedAmount = BigDecimal.ZERO;

    @Column(name = "voucher_reward_funded_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal voucherRewardFundedAmount = BigDecimal.ZERO;

    @Column(name = "commission_rate_snapshot", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal commissionRateSnapshot = new BigDecimal("0.1500");

    // PayOS Details
    @Column(name = "payos_order_code", unique = true)
    private Long payosOrderCode;

    @Column(name = "payos_payment_link_id", length = 150)
    private String payosPaymentLinkId;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "payment_purpose", length = 50)
    @Builder.Default
    private String paymentPurpose = "BOOKING";

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "actual_paid_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal actualPaidAmount = BigDecimal.ZERO;

    // Verification & lifecycle timestamps
    @Column(name = "check_in_time")
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Column(name = "claimed_late_time")
    private Instant claimedLateTime;

    @Column(name = "grace_deadline")
    private Instant graceDeadline;

    @Column(name = "service_closing_deadline")
    private Instant serviceClosingDeadline;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
