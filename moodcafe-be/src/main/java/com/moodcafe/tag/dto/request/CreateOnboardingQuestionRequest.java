package com.moodcafe.tag.dto.request;

import com.moodcafe.tag.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateOnboardingQuestionRequest {

    private UUID tagCategoryId;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String subtitle;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    private Integer displayOrder;

    private Boolean required;

    private Boolean active;

    private Integer maxSelections;
}
