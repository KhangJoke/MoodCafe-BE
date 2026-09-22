package com.moodcafe.store.dto.response;

import com.moodcafe.store.entity.enums.VisitVerificationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitVerificationResponse {

    private UUID visitVerificationId;
    private UUID storeId;
    private String storeName;
    private String imageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distanceFromStoreMeters;
    private VisitVerificationStatus status;
    private Instant capturedAt;
    private Instant verifiedAt;
    private Instant expiresAt;
    private boolean isUsed;
    private MicroSurveyQuestionDto surveyQuestion;
}
