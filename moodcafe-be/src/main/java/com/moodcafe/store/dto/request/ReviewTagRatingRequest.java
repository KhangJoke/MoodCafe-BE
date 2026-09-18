package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ReviewTagRatingRequest {

    private UUID tagId;

    private UUID storeTagId;

    @NotNull(message = "Score is required")
    @Min(value = 1, message = "Tag score must be between 1 and 5")
    @Max(value = 5, message = "Tag score must be between 1 and 5")
    private Integer score;
}
