package com.moodcafe.tag.abstraction.service;

import com.moodcafe.tag.dto.request.SubmitOnboardingRequest;
import com.moodcafe.tag.dto.response.UserPreferenceResponse;

import java.util.List;

public interface UserPreferenceService {

    List<UserPreferenceResponse> getMyPreferences();

    void submitPreferences(SubmitOnboardingRequest request);
}
