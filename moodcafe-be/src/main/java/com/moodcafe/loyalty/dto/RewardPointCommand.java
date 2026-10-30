package com.moodcafe.loyalty.dto;

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
public class RewardPointCommand {
    private UUID userId;
    private Long points;
    private String rewardType;
    private String sourceId;
    private String businessKey;
    private String description;
    private Instant expiresAt;
}

