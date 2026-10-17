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
public class StoreBookingConfigResponse {

    private UUID configId;
    private UUID storeId;
    private Boolean isBookingEnabled;
    private Boolean isTimeLimitEnabled;
    private Integer sittingDurationMins;
    private Integer cleanupBufferMins;
    private Integer gracePeriodMins;
    private Integer minAdvanceMins;
    private Integer freeCancelHours;
    private Instant createdAt;
    private Instant updatedAt;
}
