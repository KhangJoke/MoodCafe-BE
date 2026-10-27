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
public class UserTitleShowcaseResponse {
    private EquippedTitleDto equippedTitle;
    private long totalUnlocked;
    private long totalAvailable;
    private List<TitleItemDto> titles;
}
