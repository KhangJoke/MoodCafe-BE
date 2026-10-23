package com.moodcafe.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingQuoteResponse {

    private UUID quoteId;
    private Instant expiresAt;
    private String selectionFingerprint;
    private Integer pricingVersion;
    private BigDecimal grossAmount;
    private BigDecimal voucherDiscount;
    private BigDecimal customerPaidAmount;
    private String currency;
    private Boolean isEligible;
    private String ineligibilityReason;
    private Instant freeCancelDeadline;
    private Instant graceDeadline;
    private Instant serviceClosingDeadline;
}
