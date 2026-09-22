package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.VibeSurveyResponse;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveySubmissionResponse {

    private UUID visitVerificationId;
    private UUID tagId;
    private String tagName;
    private VibeSurveyResponse response;
    private boolean verifiedReviewUnlocked;
    private String message;
}
