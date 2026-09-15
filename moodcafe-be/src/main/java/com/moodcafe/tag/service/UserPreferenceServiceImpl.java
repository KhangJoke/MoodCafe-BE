package com.moodcafe.tag.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.OnboardingQuestionRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.tag.abstraction.service.UserPreferenceService;
import com.moodcafe.tag.dto.request.SubmitOnboardingAnswerItem;
import com.moodcafe.tag.dto.request.SubmitOnboardingRequest;
import com.moodcafe.tag.dto.response.UserPreferenceResponse;
import com.moodcafe.tag.entity.OnboardingQuestion;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final OnboardingQuestionRepository onboardingQuestionRepository;
    private final TagRepository tagRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    @Override
    @Transactional(readOnly = true)
    public List<UserPreferenceResponse> getMyPreferences() {
        User currentUser = currentUserService.getCurrentUser();
        List<UserPreference> preferences = userPreferenceRepository.findAllByUserId(currentUser.getUserId());

        return preferences.stream().map(pref -> {
            String tagCategoryCode = null;
            if (pref.getQuestion() != null && pref.getQuestion().getTagCategory() != null) {
                tagCategoryCode = pref.getQuestion().getTagCategory().getCode();
            }

            UUID tagId = null;
            String tagName = null;
            if (pref.getTag() != null) {
                tagId = pref.getTag().getTagId();
                tagName = pref.getTag().getName();
            }

            return UserPreferenceResponse.builder()
                    .preferenceId(pref.getPreferenceId())
                    .questionId(pref.getQuestion().getQuestionId())
                    .questionTitle(pref.getQuestion().getTitle())
                    .tagCategoryCode(tagCategoryCode)
                    .tagId(tagId)
                    .tagName(tagName)
                    .numericValue(pref.getNumericValue())
                    .isSkipped(pref.isSkipped())
                    .createdAt(pref.getCreatedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public void submitPreferences(SubmitOnboardingRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        UUID userId = currentUser.getUserId();

        // Clear previous preferences for user to allow clean update
        userPreferenceRepository.deleteAllByUserId(userId);

        List<UserPreference> preferencesToSave = new ArrayList<>();

        for (SubmitOnboardingAnswerItem item : request.getAnswers()) {
            OnboardingQuestion question = onboardingQuestionRepository.findById(item.getQuestionId())
                    .orElseThrow(() -> new AppException(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));

            if (Boolean.TRUE.equals(item.getIsSkipped())) {
                preferencesToSave.add(UserPreference.builder()
                        .userId(userId)
                        .question(question)
                        .skipped(true)
                        .build());
                continue;
            }

            if ("SLIDER".equalsIgnoreCase(question.getQuestionType())) {
                Integer sliderVal = item.getSliderValue();
                Tag matchedTag = null;
                if (item.getSelectedTagIds() != null && !item.getSelectedTagIds().isEmpty()) {
                    matchedTag = tagRepository.findById(item.getSelectedTagIds().get(0)).orElse(null);
                    if (matchedTag != null && matchedTag.getScaleValue() != null) {
                        sliderVal = matchedTag.getScaleValue();
                    }
                } else if (sliderVal != null && question.getTagCategory() != null) {
                    matchedTag = tagRepository.findByCategoryTagCategoryIdAndScaleValue(
                            question.getTagCategory().getTagCategoryId(), sliderVal).orElse(null);
                }

                preferencesToSave.add(UserPreference.builder()
                        .userId(userId)
                        .question(question)
                        .tag(matchedTag)
                        .numericValue(sliderVal != null ? sliderVal : 3)
                        .skipped(false)
                        .build());
            } else {
                if (item.getSelectedTagIds() != null && !item.getSelectedTagIds().isEmpty()) {
                    // Deduplicate selection IDs from client
                    Set<UUID> uniqueTagIds = new HashSet<>(item.getSelectedTagIds());

                    if (question.getMaxSelections() != null && uniqueTagIds.size() > question.getMaxSelections()) {
                        throw new AppException(ErrorCode.INVALID_INPUT,
                                "Số lượng lựa chọn (" + uniqueTagIds.size() + ") vượt quá giới hạn tối đa cho phép ("
                                        + question.getMaxSelections() + ") của câu hỏi: " + question.getTitle());
                    }

                    for (UUID tagId : uniqueTagIds) {
                        Tag tag = tagRepository.findById(tagId)
                                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));

                        preferencesToSave.add(UserPreference.builder()
                                .userId(userId)
                                .question(question)
                                .tag(tag)
                                .skipped(false)
                                .build());
                    }
                } else {
                    // Handled as skipped if no tags selected
                    preferencesToSave.add(UserPreference.builder()
                            .userId(userId)
                            .question(question)
                            .skipped(true)
                            .build());
                }
            }
        }

        userPreferenceRepository.saveAll(preferencesToSave);

        // Mark user onboarding as finished
        userService.setFirstLoginFalse(userId);
    }
}
