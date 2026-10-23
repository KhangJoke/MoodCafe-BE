package com.moodcafe.reservation.dto;

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
public class CreateReservationRequest {
    private UUID storeId;
    private UUID categoryId;
    private Instant bookingTime;
    @Builder.Default
    private Integer guestCount = 2;
    private UUID voucherId;
    private String notes;
    private String intentKey;
}
