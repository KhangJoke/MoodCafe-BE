package com.moodcafe.tag.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.tag.abstraction.service.OnboardingQuestionService;
import com.moodcafe.tag.abstraction.service.UserPreferenceService;
import com.moodcafe.tag.dto.request.SubmitOnboardingRequest;
import com.moodcafe.tag.dto.response.OnboardingQuestionResponse;
import com.moodcafe.tag.dto.response.UserPreferenceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingQuestionService onboardingQuestionService;
    private final UserPreferenceService userPreferenceService;

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<OnboardingQuestionResponse>>> getQuestions() {
        List<OnboardingQuestionResponse> questions = onboardingQuestionService.getActiveQuestionsForCustomer();
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @PostMapping("/answers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> submitAnswers(
            @Valid @RequestBody SubmitOnboardingRequest request
    ) {
        userPreferenceService.submitPreferences(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Onboarding preferences saved successfully"));
    }

    @GetMapping("/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserPreferenceResponse>>> getMyPreferences() {
        List<UserPreferenceResponse> preferences = userPreferenceService.getMyPreferences();
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }
}
