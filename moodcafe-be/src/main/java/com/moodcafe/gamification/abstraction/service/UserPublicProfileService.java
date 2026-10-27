package com.moodcafe.gamification.abstraction.service;

import com.moodcafe.gamification.dto.response.UserPublicProfileResponse;
import com.moodcafe.gamification.dto.response.UserPublicReviewResponse;
import com.moodcafe.gamification.dto.response.UserVibeSnapResponse;
import com.moodcafe.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserPublicProfileService {
    UserPublicProfileResponse getPublicProfile(UUID userId);
    PageResponse<UserPublicReviewResponse> getUserReviews(UUID userId, Pageable pageable);
    PageResponse<UserVibeSnapResponse> getUserVibeSnaps(UUID userId, Pageable pageable);
}
