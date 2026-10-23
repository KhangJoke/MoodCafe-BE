package com.moodcafe.booking.entity;

import com.moodcafe.auth.entity.User;
import com.moodcafe.store.entity.Store;
import com.moodcafe.voucher.entity.CustomerVoucher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "booking_quotes",
        indexes = {
                @Index(name = "idx_booking_quotes_user", columnList = "user_id, expires_at"),
                @Index(name = "idx_booking_quotes_store_category", columnList = "store_id, category_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingQuote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quote_id", nullable = false, updatable = false)
    private UUID quoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TableCategory tableCategory;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Builder.Default
    @Column(name = "guest_count", nullable = false)
    private Integer guestCount = 2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private CustomerVoucher voucher;

    @Column(name = "selection_fingerprint", nullable = false, length = 64)
    private String selectionFingerprint;

    @Builder.Default
    @Column(name = "pricing_version", nullable = false)
    private Integer pricingVersion = 1;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Builder.Default
    @Column(name = "voucher_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;

    @Column(name = "customer_paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal customerPaidAmount;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "merchant_net_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal merchantNetAmount;

    @Builder.Default
    @Column(name = "voucher_refund_funded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherRefundFundedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "voucher_reward_funded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal voucherRewardFundedAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_eligible", nullable = false)
    private Boolean isEligible = true;

    @Column(name = "ineligibility_reason", columnDefinition = "TEXT")
    private String ineligibilityReason;

    @Column(name = "free_cancel_deadline", nullable = false)
    private Instant freeCancelDeadline;

    @Column(name = "grace_deadline", nullable = false)
    private Instant graceDeadline;

    @Column(name = "service_closing_deadline", nullable = false)
    private Instant serviceClosingDeadline;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
