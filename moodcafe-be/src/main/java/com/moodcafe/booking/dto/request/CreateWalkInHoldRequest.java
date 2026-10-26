package com.moodcafe.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalkInHoldRequest {

    @NotNull(message = "ID loại bàn không được để trống")
    private UUID categoryId;

    private Integer durationMinutes;

    private String physicalTableNumber;
}
