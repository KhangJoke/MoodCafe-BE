package com.moodcafe.gamification.abstraction.service;

import com.moodcafe.gamification.dto.response.EquippedTitleDto;
import com.moodcafe.gamification.dto.response.UserTitleShowcaseResponse;

import java.util.UUID;

public interface CustomerTitleService {
    UserTitleShowcaseResponse getMyTitleShowcase();
    EquippedTitleDto getEquippedTitle(UUID userId);
    EquippedTitleDto equipTitle(UUID titleId);
    void unequipTitle();
}
