package com.moodcafe.booking.dto.request;

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
public class CheckAvailabilityRequest {

    @NotNull(message = "categoryId is required")
    private UUID categoryId;

    @NotNull(message = "startTime is required")
    private Instant startTime;
}
