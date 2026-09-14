package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreReviewRequest {

    @NotNull(message = "Overall rating is required")
    @DecimalMin(value = "1.0", message = "Overall rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Overall rating must be between 1.0 and 5.0")
    private BigDecimal overallRating;

    @Min(value = 1, message = "Quietness rating must be between 1 and 5")
    @Max(value = 5, message = "Quietness rating must be between 1 and 5")
    private Integer quietnessRating;

    @Min(value = 1, message = "Lighting rating must be between 1 and 5")
    @Max(value = 5, message = "Lighting rating must be between 1 and 5")
    private Integer lightingRating;

    @Min(value = 1, message = "Seating rating must be between 1 and 5")
    @Max(value = 5, message = "Seating rating must be between 1 and 5")
    private Integer seatingRating;

    @Min(value = 1, message = "Outlet rating must be between 1 and 5")
    @Max(value = 5, message = "Outlet rating must be between 1 and 5")
    private Integer outletRating;

    private String content;

    private UUID visitVerificationId;
}
