package com.moodcafe.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreScheduleRequest {

    @NotNull(message = "Thứ trong tuần (dayOfWeek) không được để trống")
    @Schema(description = "Thứ trong tuần: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY", example = "MONDAY")
    private DayOfWeek dayOfWeek;

    @Schema(description = "Giờ mở cửa", example = "07:00:00")
    private LocalTime openTime;

    @Schema(description = "Giờ đóng cửa", example = "22:30:00")
    private LocalTime closeTime;

    @Builder.Default
    @Schema(description = "Quán có mở cửa vào ngày này hay không", example = "true")
    private Boolean isOpen = true;
}
