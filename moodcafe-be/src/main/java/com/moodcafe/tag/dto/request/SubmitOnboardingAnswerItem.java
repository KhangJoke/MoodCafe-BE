package com.moodcafe.tag.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitOnboardingAnswerItem {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    private List<UUID> selectedTagIds;

    @Min(value = 1, message = "Slider value must be between 1 and 5")
    @Max(value = 5, message = "Slider value must be between 1 and 5")
    private Integer sliderValue;

    @Builder.Default
    private Boolean isSkipped = false;
}
