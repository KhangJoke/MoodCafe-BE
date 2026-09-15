package com.moodcafe.tag.dto.response;

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
public class UserPreferenceResponse {

    private UUID preferenceId;
    private UUID questionId;
    private String questionTitle;
    private String tagCategoryCode;
    private UUID tagId;
    private String tagName;
    private Integer numericValue;
    private Boolean isSkipped;
    private Instant createdAt;
}
