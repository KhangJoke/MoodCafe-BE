package com.moodcafe.gamification.abstraction.service;

import com.moodcafe.gamification.dto.request.CreateTitleAdminRequest;
import com.moodcafe.gamification.dto.request.UpdateTitleAdminRequest;
import com.moodcafe.gamification.dto.response.TitleAdminResponse;
import com.moodcafe.gamification.entity.enums.TitleTier;
import com.moodcafe.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AdminTitleService {
    PageResponse<TitleAdminResponse> getAllTitles(TitleTier tier, Boolean active, String search, Pageable pageable);
    List<TitleAdminResponse> getActiveTitlesForSelection();
    TitleAdminResponse getTitleById(UUID titleId);
    TitleAdminResponse createTitle(CreateTitleAdminRequest request);
    TitleAdminResponse updateTitle(UUID titleId, UpdateTitleAdminRequest request);
    void toggleTitleStatus(UUID titleId, boolean active);
    void deleteTitle(UUID titleId);
}

