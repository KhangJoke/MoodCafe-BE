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
public class PlatformFinanceTrendItemResponse {
    private String label;
    private BigDecimal subscriptionRevenue;
    private BigDecimal bookingCommissionRevenue;
    private BigDecimal totalRevenue;
    private BigDecimal voucherExpenses;
    private BigDecimal netIncome;
}

