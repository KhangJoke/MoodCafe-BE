package com.moodcafe.tag.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.OnboardingQuestionRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.abstraction.repository.UserPreferenceRepository;
import com.moodcafe.tag.dto.request.SubmitOnboardingAnswerItem;
import com.moodcafe.tag.dto.request.SubmitOnboardingRequest;
import com.moodcafe.tag.dto.response.UserPreferenceResponse;
import com.moodcafe.tag.entity.OnboardingQuestion;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.UserPreference;
import com.moodcafe.tag.entity.enums.QuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceImplTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private OnboardingQuestionRepository onboardingQuestionRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserPreferenceServiceImpl userPreferenceService;

    private UUID userId;
    private User currentUser;
    private UUID questionId;
    private OnboardingQuestion question;
    private UUID tagId;
    private Tag tag;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        currentUser = User.builder()
                .userId(userId)
                .email("customer@example.com")
                .firstLogin(true)
                .build();

        questionId = UUID.randomUUID();
        TagCategory tagCategory = TagCategory.builder()
                .code("VIBE")
                .name("Vibe & Phong cách")
                .build();

        question = OnboardingQuestion.builder()
                .questionId(questionId)
                .title("Gu không gian?")
                .tagCategory(tagCategory)
                .questionType(QuestionType.MULTI_SELECT)
                .maxSelections(3)
                .build();

        tagId = UUID.randomUUID();
        tag = Tag.builder()
                .tagId(tagId)
                .name("Vintage")
                .category(tagCategory)
                .build();
    }

    @Test
    @DisplayName("getMyPreferences - returns preferences for current user")
    void getMyPreferences_success() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        UserPreference pref = UserPreference.builder()
                .preferenceId(UUID.randomUUID())
                .userId(userId)
                .question(question)
                .tag(tag)
                .skipped(false)
                .build();

        when(userPreferenceRepository.findAllByUserId(userId)).thenReturn(List.of(pref));

        List<UserPreferenceResponse> results = userPreferenceService.getMyPreferences();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionTitle()).isEqualTo("Gu không gian?");
        assertThat(results.get(0).getTagName()).isEqualTo("Vintage");
    }

    @Test
    @DisplayName("submitPreferences - saves answers and marks onboarding complete")
    void submitPreferences_success() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        UUID sliderQId = UUID.randomUUID();
        OnboardingQuestion sliderQ = OnboardingQuestion.builder()
                .questionId(sliderQId)
                .questionType(QuestionType.SLIDER)
                .build();

        when(onboardingQuestionRepository.findById(sliderQId)).thenReturn(Optional.of(sliderQ));
        when(onboardingQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));

        SubmitOnboardingRequest request = SubmitOnboardingRequest.builder()
                .answers(List.of(
                        SubmitOnboardingAnswerItem.builder()
                                .questionId(sliderQId)
                                .sliderValue(4)
                                .build(),
                        SubmitOnboardingAnswerItem.builder()
                                .questionId(questionId)
                                .selectedTagIds(List.of(tagId))
                                .build()
                ))
                .build();

        userPreferenceService.submitPreferences(request);

        verify(userPreferenceRepository).deleteAllByUserId(userId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserPreference>> captor = ArgumentCaptor.forClass(List.class);
        verify(userPreferenceRepository).saveAll(captor.capture());

        List<UserPreference> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getNumericValue()).isEqualTo(4);
        assertThat(saved.get(1).getTag().getName()).isEqualTo("Vintage");

        verify(userService).setFirstLoginFalse(userId);
    }

    @Test
    @DisplayName("submitPreferences - throws when selected tags exceed maxSelections")
    void submitPreferences_exceedMaxSelections() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        UUID tag2 = UUID.randomUUID();
        UUID tag3 = UUID.randomUUID();
        UUID tag4 = UUID.randomUUID();

        OnboardingQuestion limitedQuestion = OnboardingQuestion.builder()
                .questionId(questionId)
                .title("Gu không gian?")
                .questionType(QuestionType.MULTI_SELECT)
                .maxSelections(2)
                .build();

        when(onboardingQuestionRepository.findById(questionId)).thenReturn(Optional.of(limitedQuestion));

        SubmitOnboardingRequest request = SubmitOnboardingRequest.builder()
                .answers(List.of(
                        SubmitOnboardingAnswerItem.builder()
                                .questionId(questionId)
                                .selectedTagIds(List.of(tagId, tag2, tag3, tag4))
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> userPreferenceService.submitPreferences(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("submitPreferences - throws when question does not exist")
    void submitPreferences_questionNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(onboardingQuestionRepository.findById(questionId)).thenReturn(Optional.empty());

        SubmitOnboardingRequest request = SubmitOnboardingRequest.builder()
                .answers(List.of(
                        SubmitOnboardingAnswerItem.builder()
                                .questionId(questionId)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> userPreferenceService.submitPreferences(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));
    }
}
