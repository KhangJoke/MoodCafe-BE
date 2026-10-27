package com.moodcafe.gamification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerQuestBoardResponse {
    private String todayPeriodKey;
    private String monthPeriodKey;
    private QuestSummaryDto summary;
    private List<QuestProgressItemDto> dailyQuests;
    private List<QuestProgressItemDto> milestoneQuests;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestSummaryDto {
        private int dailyCompletedCount;
        private int dailyTotalCount;
        private int claimableTotalCount;
        private int rewardedTotalCount;
    }
}
