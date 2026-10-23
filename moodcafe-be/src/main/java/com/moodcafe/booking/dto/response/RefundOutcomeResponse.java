package com.moodcafe.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundOutcomeResponse {

    private Long actualPaidRefundPoints;
    private String voucherRestorationStatus;
    private Instant processedAt;
    private String eventId;
}
