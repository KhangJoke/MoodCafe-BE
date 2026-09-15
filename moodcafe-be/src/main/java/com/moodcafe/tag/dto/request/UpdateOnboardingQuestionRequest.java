package com.moodcafe.tag.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class UpdateOnboardingQuestionRequest {

    private UUID tagCategoryId;

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String subtitle;

    @Pattern(regexp = "^(SINGLE_SELECT|MULTI_SELECT|SLIDER)$", message = "Question type must be SINGLE_SELECT, MULTI_SELECT, or SLIDER")
    private String questionType;

    private Integer displayOrder;

    private Boolean required;

    private Boolean active;

    private Integer maxSelections;
}
