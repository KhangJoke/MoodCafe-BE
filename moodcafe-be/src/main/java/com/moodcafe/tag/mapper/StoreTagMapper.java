package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreTagMapper {

    @Mapping(target = "storeId", source = "storeId")
    @Mapping(target = "storeName", ignore = true)
    @Mapping(target = "tagId", source = "tag.tagId")
    @Mapping(target = "tagName", source = "tag.name")
    @Mapping(target = "category", source = "tag.category.name")
    @Mapping(target = "categoryCode", source = "tag.category.code")
    @Mapping(target = "averageScore", source = "avgScore")
    @Mapping(target = "reviewCount", source = "reviewCount")
    StoreTagResponse toResponse(StoreTag storeTag);
}
