package com.moodcafe.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDeskSummaryResponse {

    private long totalTodayBookings;
    private long pendingArrivalCount;
    private long checkedInCount;
    private long tableExpiredCount;
    private long completedCount;
    private long claimedLateCount;
    private long totalAppQuota;
}
