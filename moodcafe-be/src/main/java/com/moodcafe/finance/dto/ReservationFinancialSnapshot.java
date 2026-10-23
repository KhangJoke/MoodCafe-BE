package com.moodcafe.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationFinancialSnapshot {

    /**
     * Total minimum spend / table gross value at counter (gross_amount).
     */
    private BigDecimal grossAmount;

    /**
     * Discount deducted via redeemed voucher (voucher_discount).
     */
    private BigDecimal voucherDiscount;

    /**
     * Real payment required from customer via PayOS (customer_paid_amount = gross - voucher_discount).
     */
    private BigDecimal customerPaidAmount;

    /**
     * Snapshot commission rate (e.g. 0.15 = 15%).
     */
    private BigDecimal commissionRate;

    /**
     * Platform gross commission (commission_amount = gross * commission_rate).
     */
    private BigDecimal commissionAmount;

    /**
     * Guaranteed net amount merchant receives to their wallet (merchant_net_amount = gross - commission).
     * Merchant is NEVER penalized by vouchers.
     */
    private BigDecimal merchantNetAmount;

    /**
     * Net revenue retained by platform before external fees
     * (platform_net_revenue = commission_amount - voucher_reward_funded_amount).
     */
    private BigDecimal platformNetRevenue;

    /**
     * Portion of voucher funded by customer's prior cash refund obligations.
     */
    private BigDecimal voucherRefundFundedAmount;

    /**
     * Portion of voucher funded by platform reward / marketing budget.
     */
    private BigDecimal voucherRewardFundedAmount;
}
