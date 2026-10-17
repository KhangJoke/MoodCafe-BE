package com.moodcafe.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotAvailabilityResponse {

    private UUID categoryId;
    private String categoryName;
    private Instant startTime;
    private Instant endTime;
    private Integer totalQuota;
    private Integer occupiedCount;
    private Integer availableCount;
    private Boolean isAvailable;
    private Long minSpendAmount;
}
