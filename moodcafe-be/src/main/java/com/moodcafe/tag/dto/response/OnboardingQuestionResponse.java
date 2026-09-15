package com.moodcafe.tag.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingQuestionResponse {

    private UUID questionId;
    private UUID tagCategoryId;
    private String tagCategoryCode;
    private String tagCategoryName;
    private String title;
    private String subtitle;
    private String questionType;
    private Integer displayOrder;
    private Boolean required;
    private Boolean active;
    private Integer maxSelections;
    private List<TagOptionResponse> options;
    private Instant createdAt;
    private Instant updatedAt;
}
