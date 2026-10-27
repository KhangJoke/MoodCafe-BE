package com.moodcafe.gamification.dto.response;

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
public class UserPublicProfileResponse {
    private UUID userId;
    private String fullName;
    private String avatarUrl;
    private Instant joinedAt;
    private String joinedDisplay;
    private EquippedTitleDto equippedTitle;
    private UserStatsDto stats;
    private List<EquippedTitleDto> unlockedTitles;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserStatsDto {
        private long totalReviews;
        private long totalVibeSnaps;
        private long totalTitlesUnlocked;
    }
}
