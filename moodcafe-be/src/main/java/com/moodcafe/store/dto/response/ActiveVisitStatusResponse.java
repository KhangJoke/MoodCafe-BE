package com.moodcafe.store.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActiveVisitStatusResponse {

    private boolean hasActiveVisit;
    private UUID visitVerificationId;
    private boolean surveyCompleted;
    private Instant expiresAt;
    private Long remainingMinutes;
}
