package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreScheduleResponse {

    private UUID scheduleId;
    private DayOfWeek dayOfWeek;
    private String dayNameVi;
    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean isOpen;
}
