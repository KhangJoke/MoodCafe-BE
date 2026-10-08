package com.moodcafe.subscription.dto.response;

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
public class RevenueTrendItemResponse {

    private String date;
    private BigDecimal revenue;
    private long transactionCount;
}
