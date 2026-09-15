package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.request.CreateTagCategoryRequest;
import com.moodcafe.tag.dto.request.UpdateTagCategoryRequest;
import com.moodcafe.tag.dto.response.TagCategoryResponse;
import com.moodcafe.tag.entity.TagCategory;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TagCategoryMapper {

    TagCategoryResponse toResponse(TagCategory category);

    @Mapping(target = "tagCategoryId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    TagCategory toEntity(CreateTagCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tagCategoryId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateTagCategoryRequest request, @MappingTarget TagCategory category);
}
