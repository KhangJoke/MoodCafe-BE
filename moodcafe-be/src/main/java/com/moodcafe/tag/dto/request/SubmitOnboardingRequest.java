package com.moodcafe.tag.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitOnboardingRequest {

    @NotEmpty(message = "Answers cannot be empty")
    @Valid
    private List<SubmitOnboardingAnswerItem> answers;
}
