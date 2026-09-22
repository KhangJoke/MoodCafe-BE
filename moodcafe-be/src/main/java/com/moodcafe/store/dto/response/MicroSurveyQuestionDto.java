package com.moodcafe.store.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MicroSurveyQuestionDto {

    private UUID tagId;
    private String tagName;
    private String categoryName;
    private String imageUrl;
    private String questionText;
    private List<String> options;
}
