package com.moodcafe.finance.dto;

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
public class PlatformRevenueEntryResponse {
    private UUID entryId;
    private String source; // SUBSCRIPTION_PAYMENT or BOOKING_COMMISSION
    private String referenceCode;
    private String description;
    private BigDecimal amount;
    private String storeName;
    private String payerName;
    private Instant occurredAt;
}

