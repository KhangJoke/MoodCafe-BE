package com.moodcafe.tag.controller;

import com.moodcafe.shared.response.ApiResponse;
import com.moodcafe.tag.abstraction.service.OnboardingQuestionService;
import com.moodcafe.tag.dto.request.CreateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.request.UpdateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.response.OnboardingQuestionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/onboarding-questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOnboardingQuestionController {

    private final OnboardingQuestionService onboardingQuestionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OnboardingQuestionResponse>>> getAllQuestions() {
        List<OnboardingQuestionResponse> questions = onboardingQuestionService.getAllQuestionsAdmin();
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @GetMapping("/{questionId}")
    public ResponseEntity<ApiResponse<OnboardingQuestionResponse>> getQuestionById(@PathVariable UUID questionId) {
        OnboardingQuestionResponse question = onboardingQuestionService.getQuestionById(questionId);
        return ResponseEntity.ok(ApiResponse.success(question));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingQuestionResponse>> createQuestion(
            @Valid @RequestBody CreateOnboardingQuestionRequest request
    ) {
        OnboardingQuestionResponse question = onboardingQuestionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(question, "Onboarding question created successfully"));
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<ApiResponse<OnboardingQuestionResponse>> updateQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateOnboardingQuestionRequest request
    ) {
        OnboardingQuestionResponse question = onboardingQuestionService.updateQuestion(questionId, request);
        return ResponseEntity.ok(ApiResponse.success(question, "Onboarding question updated successfully"));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(@PathVariable UUID questionId) {
        onboardingQuestionService.deleteQuestion(questionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Onboarding question deleted successfully"));
    }
}
