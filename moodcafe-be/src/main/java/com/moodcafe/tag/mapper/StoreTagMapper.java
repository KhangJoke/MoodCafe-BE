package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.response.StoreTagResponse;
import com.moodcafe.tag.entity.StoreTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreTagMapper {

    @Mapping(target = "storeId", source = "storeId")
    @Mapping(target = "storeName", ignore = true)
    @Mapping(target = "tagId", source = "tag.tagId")
    @Mapping(target = "tagName", source = "tag.name")
    @Mapping(target = "category", source = "tag.category.name")
    @Mapping(target = "categoryCode", source = "tag.category.code")
    @Mapping(target = "approvalMode", source = "tag.category.approvalMode")
    @Mapping(target = "controlType", source = "tag.category.controlType")
    @Mapping(target = "imageUrl", source = "tag.imageUrl")
    @Mapping(target = "averageScore", source = "avgScore")
    @Mapping(target = "reviewCount", source = "reviewCount")
    @Mapping(target = "proofImageUrls", source = "proofImageUrl", qualifiedByName = "mapProofImageUrls")
    StoreTagResponse toResponse(StoreTag storeTag);

    @Named("mapProofImageUrls")
    default List<String> mapProofImageUrls(String proofImageUrl) {
        if (proofImageUrl == null || proofImageUrl.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(proofImageUrl.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
