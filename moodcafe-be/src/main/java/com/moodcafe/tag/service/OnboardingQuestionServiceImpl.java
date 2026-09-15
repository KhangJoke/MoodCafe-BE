package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.OnboardingQuestionRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.service.OnboardingQuestionService;
import com.moodcafe.tag.dto.request.CreateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.request.UpdateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.response.OnboardingQuestionResponse;
import com.moodcafe.tag.dto.response.TagOptionResponse;
import com.moodcafe.tag.entity.OnboardingQuestion;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.mapper.OnboardingQuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingQuestionServiceImpl implements OnboardingQuestionService {

    private final OnboardingQuestionRepository onboardingQuestionRepository;
    private final TagCategoryRepository tagCategoryRepository;
    private final TagRepository tagRepository;
    private final OnboardingQuestionMapper onboardingQuestionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingQuestionResponse> getAllQuestionsAdmin() {
        return onboardingQuestionRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapQuestionWithOptions)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingQuestionResponse> getActiveQuestionsForCustomer() {
        return onboardingQuestionRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapQuestionWithOptions)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingQuestionResponse getQuestionById(UUID questionId) {
        OnboardingQuestion question = onboardingQuestionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));
        return mapQuestionWithOptions(question);
    }

    @Override
    @Transactional
    public OnboardingQuestionResponse createQuestion(CreateOnboardingQuestionRequest request) {
        OnboardingQuestion question = onboardingQuestionMapper.toEntity(request);

        if (request.getTagCategoryId() != null) {
            TagCategory tagCategory = tagCategoryRepository.findById(request.getTagCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));
            question.setTagCategory(tagCategory);
        }

        question = onboardingQuestionRepository.save(question);
        return mapQuestionWithOptions(question);
    }

    @Override
    @Transactional
    public OnboardingQuestionResponse updateQuestion(UUID questionId, UpdateOnboardingQuestionRequest request) {
        OnboardingQuestion question = onboardingQuestionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));

        if (request.getTagCategoryId() != null) {
            TagCategory tagCategory = tagCategoryRepository.findById(request.getTagCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.TAG_CATEGORY_NOT_FOUND));
            question.setTagCategory(tagCategory);
        }

        onboardingQuestionMapper.updateEntity(request, question);
        question = onboardingQuestionRepository.save(question);
        return mapQuestionWithOptions(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        OnboardingQuestion question = onboardingQuestionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));
        question.setActive(false);
        onboardingQuestionRepository.save(question);
    }

    private OnboardingQuestionResponse mapQuestionWithOptions(OnboardingQuestion question) {
        OnboardingQuestionResponse response = onboardingQuestionMapper.toResponse(question);

        if (question.getTagCategory() != null) {
            List<Tag> tags = tagRepository.findAllByCategoryTagCategoryIdAndActiveTrue(question.getTagCategory().getTagCategoryId());
            if (tags.isEmpty() && question.getTagCategory().getCode() != null) {
                tags = tagRepository.findAllByCategoryCodeAndActiveTrue(question.getTagCategory().getCode());
            }

            List<TagOptionResponse> options = tags.stream()
                    .sorted((a, b) -> {
                        if (a.getScaleValue() != null && b.getScaleValue() != null) {
                            return a.getScaleValue().compareTo(b.getScaleValue());
                        }
                        return a.getName().compareToIgnoreCase(b.getName());
                    })
                    .map(t -> TagOptionResponse.builder()
                            .tagId(t.getTagId())
                            .name(t.getName())
                            .description(t.getDescription())
                            .scaleValue(t.getScaleValue())
                            .build())
                    .toList();
            response.setOptions(options);
        } else {
            response.setOptions(new ArrayList<>());
        }

        return response;
    }
}
