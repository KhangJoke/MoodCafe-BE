package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.request.CreateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.request.UpdateOnboardingQuestionRequest;
import com.moodcafe.tag.dto.response.OnboardingQuestionResponse;
import com.moodcafe.tag.entity.OnboardingQuestion;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface OnboardingQuestionMapper {

    @Mapping(target = "tagCategoryId", source = "tagCategory.tagCategoryId")
    @Mapping(target = "tagCategoryCode", source = "tagCategory.code")
    @Mapping(target = "tagCategoryName", source = "tagCategory.name")
    @Mapping(target = "options", ignore = true)
    OnboardingQuestionResponse toResponse(OnboardingQuestion question);

    @Mapping(target = "questionId", ignore = true)
    @Mapping(target = "tagCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OnboardingQuestion toEntity(CreateOnboardingQuestionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "questionId", ignore = true)
    @Mapping(target = "tagCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(UpdateOnboardingQuestionRequest request, @MappingTarget OnboardingQuestion question);
}
