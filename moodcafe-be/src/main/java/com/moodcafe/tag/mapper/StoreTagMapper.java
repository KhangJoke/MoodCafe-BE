package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreTagMapper {

    @Mapping(target = "tagId", source = "tag.tagId")
    @Mapping(target = "tagName", source = "tag.name")
    @Mapping(target = "category", source = "tag.category.name")
    @Mapping(target = "categoryCode", source = "tag.category.code")
    StoreTagResponse toResponse(StoreTag storeTag);
}
