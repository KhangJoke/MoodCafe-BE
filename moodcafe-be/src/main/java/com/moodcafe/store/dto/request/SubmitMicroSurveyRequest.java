package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.VibeSurveyResponse;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitMicroSurveyRequest {

    @NotNull(message = "Tag ID is required")
    private UUID tagId;

    @NotNull(message = "Response is required")
    private VibeSurveyResponse response;
}
