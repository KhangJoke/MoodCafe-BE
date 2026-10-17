package com.moodcafe.booking.dto.response;

import com.moodcafe.booking.entity.enums.SlotStatus;
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
public class TimeSlotDTO {

    private String time; // e.g. "09:00"

    private Instant startTime;

    private Instant endTime;

    private int availableCount;

    private int occupiedCount;

    private SlotStatus status;

    private Integer availableDurationMins; // e.g. 45, 60, 120

    private String nextBookingTime; // e.g. "10:00"

    private String note;
}
