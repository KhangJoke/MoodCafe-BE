package com.moodcafe.tag.abstraction.service;

import com.moodcafe.tag.dto.request.CreateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.request.UpdateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.response.OnboardingQuestionResponse;

import java.util.List;
import java.util.UUID;

public interface OnboardingQuestionService {

    List<OnboardingQuestionResponse> getAllQuestionsAdmin();

    List<OnboardingQuestionResponse> getActiveQuestionsForCustomer();

    OnboardingQuestionResponse getQuestionById(UUID questionId);

    OnboardingQuestionResponse createQuestion(CreateOnboardingQuestionRequest request);

    OnboardingQuestionResponse updateQuestion(UUID questionId, UpdateOnboardingQuestionRequest request);

    void deleteQuestion(UUID questionId);
}
