package com.moodcafe.loyalty.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerWalletResponse {
    private UUID userId;
    private Long pointsBalance;
    @Builder.Default
    private Long conversionRateVnd = 1L;
    private Long spendablePoints;
}

