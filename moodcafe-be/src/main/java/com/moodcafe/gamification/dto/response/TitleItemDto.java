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
public class TitleItemDto {
    private UUID titleId;
    private String code;
    private String name;
    private String description;
    private String tier;
    private String colorHex;
    private String iconUrl;
    private boolean isUnlocked;
    private boolean isEquipped;
    private Instant unlockedAt;
    private String unlockCondition;
}
