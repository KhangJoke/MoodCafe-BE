package com.moodcafe.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreDailyScheduleResponse {

    private UUID storeId;

    private String storeName;

    private LocalDate date;

    private Boolean isBookingEnabled;

    private Boolean isTimeLimitEnabled;

    private Integer sittingDurationMins;

    private Integer cleanupBufferMins;

    private Integer minAdvanceMins;

    private Integer gracePeriodMins;

    private Integer freeCancelHours;

    private String openingTime;

    private String closingTime;

    private List<CategoryScheduleDTO> categories;
}
