package com.moodcafe.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformFinanceSummaryResponse {

    private LocalDate from;
    private LocalDate to;

    private FinancialSnapshot allTime;
    private FinancialSnapshot period;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialSnapshot {
        // Revenue (Doanh thu)
        private BigDecimal totalRevenue;
        private BigDecimal subscriptionRevenue;
        private BigDecimal bookingCommissionRevenue;
        private BigDecimal pendingBookingCommission;
        private BigDecimal reversedBookingCommission;
        private BigDecimal subscriptionSharePercent;
        private BigDecimal bookingCommissionSharePercent;
        private long subscriptionPaymentCount;
        private long settledBookingCount;

        // Expenses (Chi phí tạo voucher từ doanh thu)
        private BigDecimal totalExpenses;
        private BigDecimal expensesFromSubscription;
        private BigDecimal expensesFromBookingCommission;
        private BigDecimal expensesFromGeneralRevenue;
        private long revenueVouchersIssuedCount;
        private long revenueVouchersUsedCount;
        private BigDecimal revenueVouchersUsedValueVnd;

        // Income / Net Income (Lợi nhuận ròng = Revenue - Expenses)
        private BigDecimal netIncome;
        private BigDecimal profitMarginPercent;

        // Available balance per source (for Admin reference when creating campaigns)
        private BigDecimal availableSubscriptionNetBalance;
        private BigDecimal availableBookingCommissionNetBalance;
    }
}

