package com.moodcafe.gamification.dto.response;

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
public class UserVibeSnapResponse {
    private UUID visitVerificationId;
    private UUID storeId;
    private String storeName;
    private String imageUrl;
    private String status;
    private Instant capturedAt;
}
