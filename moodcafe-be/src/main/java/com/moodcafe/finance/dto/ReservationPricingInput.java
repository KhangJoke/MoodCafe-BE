package com.moodcafe.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationPricingInput {
    private UUID storeId;
    private UUID categoryId;
    private BigDecimal grossAmount;
    private UUID userId;
    private UUID voucherId;

    // Optional pre-resolved voucher details (if voucher validation already performed)
    @Builder.Default
    private BigDecimal voucherDiscount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal voucherRefundFunded = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal voucherRewardFunded = BigDecimal.ZERO;
}
