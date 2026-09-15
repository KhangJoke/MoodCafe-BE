package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.TagResponse;
import com.moodcafe.tag.entity.Tag;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TagMapper {

    @Mapping(target = "tagCategoryId", source = "category.tagCategoryId")
    @Mapping(target = "categoryCode", source = "category.code")
    @Mapping(target = "categoryName", source = "category.name")
    TagResponse toResponse(Tag tag);

    @Mapping(target = "tagId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    Tag toEntity(CreateTagRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tagId", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateTagRequest request, @MappingTarget Tag tag);
}
