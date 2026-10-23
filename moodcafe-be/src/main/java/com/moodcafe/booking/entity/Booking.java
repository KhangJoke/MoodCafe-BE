package com.moodcafe.booking.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.booking.entity.enums.BookingStatus;
import com.moodcafe.store.entity.Store;
import com.moodcafe.voucher.entity.CustomerVoucher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(name = "idx_bookings_store_id", columnList = "store_id"),
                @Index(name = "idx_bookings_category_id", columnList = "category_id"),
                @Index(name = "idx_bookings_user_id", columnList = "user_id"),
                @Index(name = "idx_bookings_code", columnList = "booking_code"),
                @Index(name = "idx_bookings_status_start_time", columnList = "status, start_time"),
                @Index(name = "idx_bookings_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_bookings_payment_order_code", columnList = "payment_order_code")
        }
)
@SQLDelete(sql = "UPDATE bookings SET is_deleted = true WHERE booking_id = ?")
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "booking_code", nullable = false, length = 32)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TableCategory tableCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "customer_email", length = 255)
    private String customerEmail;

    @Builder.Default
    @Column(name = "guest_count", nullable = false)
    private Integer guestCount = 2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private BookingQuote quote;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Builder.Default
    @Column(name = "min_spend_amount", nullable = false)
    private Long minSpendAmount = 0L;

    // 5 Financial Snapshot Fields (guaranteed immutable after creation)
    @Builder.Default
    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "voucher_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "customer_paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal customerPaidAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "merchant_net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal merchantNetAmount = BigDecimal.ZERO;

    // Voucher & Funding breakdown
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private CustomerVoucher voucher;

    @Builder.Default
    @Column(name = "voucher_refund_funded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherRefundFundedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "voucher_reward_funded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherRewardFundedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_rate_snapshot", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRateSnapshot = new BigDecimal("0.1500");

    @Builder.Default
    @Column(name = "actual_paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal actualPaidAmount = BigDecimal.ZERO;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "payment_url", length = 500)
    private String paymentUrl;

    @Column(name = "payment_order_code")
    private Long paymentOrderCode;

    /**
     * Server-side hold deadline for the booking. Recreating a PayOS link must
     * never extend this value.
     */
    @Column(name = "payment_expires_at")
    private Instant paymentExpiresAt;

    @Column(name = "qr_code_token", length = 255)
    private String qrCodeToken;

    @Column(name = "free_cancel_deadline")
    private Instant freeCancelDeadline;

    @Column(name = "grace_deadline")
    private Instant graceDeadline;

    @Column(name = "service_closing_deadline")
    private Instant serviceClosingDeadline;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "claimed_late_at")
    private Instant claimedLateAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Builder.Default
    @Column(name = "refund_processed", nullable = false)
    private Boolean refundProcessed = false;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Version
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Long version = 0L;

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
