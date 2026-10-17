package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class UpdateStoreBookingConfigRequest {

    @NotNull(message = "isBookingEnabled is required")
    private Boolean isBookingEnabled;

    @NotNull(message = "isTimeLimitEnabled is required")
    private Boolean isTimeLimitEnabled;

    @NotNull(message = "sittingDurationMins is required")
    @Min(value = 15, message = "Thời gian ngồi tối thiểu 15 phút")
    @Max(value = 600, message = "Thời gian ngồi tối đa 600 phút")
    private Integer sittingDurationMins;

    @NotNull(message = "cleanupBufferMins is required")
    @Min(value = 0, message = "Thời gian dọn bàn không thể âm")
    @Max(value = 60, message = "Thời gian dọn bàn tối đa 60 phút")
    private Integer cleanupBufferMins;

    @NotNull(message = "gracePeriodMins is required")
    @Min(value = 5, message = "Thời gian giữ bàn tối thiểu 5 phút")
    @Max(value = 180, message = "Thời gian giữ bàn tối đa 180 phút")
    private Integer gracePeriodMins;

    @NotNull(message = "minAdvanceMins is required")
    @Min(value = 0, message = "Thời gian đặt trước tối thiểu không thể âm")
    @Max(value = 2880, message = "Thời gian đặt trước tối đa 2 ngày (2880 phút)")
    private Integer minAdvanceMins;

    @NotNull(message = "freeCancelHours is required")
    @Min(value = 0, message = "Thời gian hủy miễn phí không thể âm")
    @Max(value = 72, message = "Thời gian hủy miễn phí tối đa 72 giờ")
    private Integer freeCancelHours;
}
