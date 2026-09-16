package com.moodcafe.tag.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.tag.abstraction.repository.OnboardingQuestionRepository;
import com.moodcafe.tag.abstraction.repository.TagCategoryRepository;
import com.moodcafe.tag.abstraction.repository.TagRepository;
import com.moodcafe.tag.dto.request.CreateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.response.OnboardingQuestionResponse;
import com.moodcafe.tag.entity.OnboardingQuestion;
import com.moodcafe.tag.entity.Tag;
import com.moodcafe.tag.entity.TagCategory;
import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import com.moodcafe.tag.entity.enums.QuestionType;
import com.moodcafe.tag.mapper.OnboardingQuestionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class OnboardingQuestionServiceImplTest {

    @Mock
    private OnboardingQuestionRepository onboardingQuestionRepository;

    @Mock
    private TagCategoryRepository tagCategoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private OnboardingQuestionMapper onboardingQuestionMapper;

    @InjectMocks
    private OnboardingQuestionServiceImpl questionService;

    private UUID questionId;
    private UUID tagCategoryId;
    private TagCategory tagCategory;
    private OnboardingQuestion question;
    private OnboardingQuestionResponse questionResponse;
    private Tag vibeTag;

    @BeforeEach
    void setUp() {
        questionId = UUID.randomUUID();
        tagCategoryId = UUID.randomUUID();

        tagCategory = TagCategory.builder()
                .tagCategoryId(tagCategoryId)
                .code("VIBE")
                .name("Vibe & Phong cách")
                .approvalMode(ApprovalMode.OWNER_REQUEST)
                .controlType(ControlType.TAG_LIST)
                .active(true)
                .build();

        question = OnboardingQuestion.builder()
                .questionId(questionId)
                .tagCategory(tagCategory)
                .title("Gu không gian yêu thích?")
                .questionType(QuestionType.MULTI_SELECT)
                .displayOrder(1)
                .required(true)
                .active(true)
                .maxSelections(3)
                .build();

        questionResponse = OnboardingQuestionResponse.builder()
                .questionId(questionId)
                .tagCategoryId(tagCategoryId)
                .title("Gu không gian yêu thích?")
                .questionType(QuestionType.MULTI_SELECT)
                .displayOrder(1)
                .required(true)
                .active(true)
                .maxSelections(3)
                .build();

        vibeTag = Tag.builder()
                .tagId(UUID.randomUUID())
                .name("Vintage")
                .description("Cổ điển")
                .category(tagCategory)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("getActiveQuestionsForCustomer - returns active questions with options")
    void getActiveQuestionsForCustomer_returnsQuestionsWithOptions() {
        when(onboardingQuestionRepository.findAllByActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(question));
        when(onboardingQuestionMapper.toResponse(question))
                .thenReturn(questionResponse);
        when(tagRepository.findAllByCategoryTagCategoryIdAndActiveTrue(tagCategoryId))
                .thenReturn(List.of(vibeTag));

        List<OnboardingQuestionResponse> result = questionService.getActiveQuestionsForCustomer();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Gu không gian yêu thích?");
        assertThat(result.get(0).getOptions()).hasSize(1);
        assertThat(result.get(0).getOptions().get(0).getName()).isEqualTo("Vintage");
    }

    @Test
    @DisplayName("getQuestionById - returns question when found")
    void getQuestionById_found() {
        when(onboardingQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(onboardingQuestionMapper.toResponse(question)).thenReturn(questionResponse);
        when(tagRepository.findAllByCategoryTagCategoryIdAndActiveTrue(tagCategoryId))
                .thenReturn(List.of(vibeTag));

        OnboardingQuestionResponse result = questionService.getQuestionById(questionId);

        assertThat(result).isNotNull();
        assertThat(result.getQuestionId()).isEqualTo(questionId);
    }

    @Test
    @DisplayName("getQuestionById - throws when question not found")
    void getQuestionById_notFound() {
        when(onboardingQuestionRepository.findById(questionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestionById(questionId))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.ONBOARDING_QUESTION_NOT_FOUND));
    }

    @Test
    @DisplayName("createQuestion - successfully creates question linked with tagCategory")
    void createQuestion_success() {
        CreateOnboardingQuestionRequest request = CreateOnboardingQuestionRequest.builder()
                .tagCategoryId(tagCategoryId)
                .title("Gu không gian yêu thích?")
                .questionType(QuestionType.MULTI_SELECT)
                .displayOrder(1)
                .required(true)
                .maxSelections(3)
                .build();

        when(onboardingQuestionMapper.toEntity(request)).thenReturn(question);
        when(tagCategoryRepository.findById(tagCategoryId)).thenReturn(Optional.of(tagCategory));
        when(onboardingQuestionRepository.save(any(OnboardingQuestion.class))).thenReturn(question);
        when(onboardingQuestionMapper.toResponse(question)).thenReturn(questionResponse);

        OnboardingQuestionResponse result = questionService.createQuestion(request);

        assertThat(result).isNotNull();
        verify(onboardingQuestionRepository).save(question);
    }

    @Test
    @DisplayName("createQuestion - throws when tagCategory not found")
    void createQuestion_tagCategoryNotFound() {
        CreateOnboardingQuestionRequest request = CreateOnboardingQuestionRequest.builder()
                .tagCategoryId(tagCategoryId)
                .title("Gu không gian yêu thích?")
                .questionType(QuestionType.MULTI_SELECT)
                .build();

        when(onboardingQuestionMapper.toEntity(request)).thenReturn(question);
        when(tagCategoryRepository.findById(tagCategoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TAG_CATEGORY_NOT_FOUND));
    }

    @Test
    @DisplayName("deleteQuestion - sets question active to false")
    void deleteQuestion_softDelete() {
        when(onboardingQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(onboardingQuestionRepository.save(question)).thenReturn(question);

        questionService.deleteQuestion(questionId);

        assertThat(question.isActive()).isFalse();
        verify(onboardingQuestionRepository).save(question);
    }
}
