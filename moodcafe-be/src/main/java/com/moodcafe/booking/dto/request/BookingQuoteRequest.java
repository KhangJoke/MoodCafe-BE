package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class BookingQuoteRequest {

    @NotNull(message = "storeId không được để trống")
    private UUID storeId;

    @NotNull(message = "categoryId không được để trống")
    private UUID categoryId;

    @NotNull(message = "startTime không được để trống")
    private Instant startTime;

    @Builder.Default
    @NotNull(message = "guestCount không được để trống")
    @Min(value = 1, message = "Số lượng khách tối thiểu là 1")
    private Integer guestCount = 2;

    private UUID voucherId;
}
