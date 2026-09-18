package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.response.ReviewTagResponse;
import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.StoreReview;
import com.moodcafe.store.entity.TagRating;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface StoreReviewMapper {

    @Mapping(source = "store.storeId", target = "storeId")
    @Mapping(source = "store.name", target = "storeName")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.fullName", target = "userFullName")
    @Mapping(source = "user.avatarUrl", target = "userAvatarUrl")
    @Mapping(source = "images", target = "imageUrls", qualifiedByName = "mapImagesToUrls")
    @Mapping(source = "tagRatings", target = "tagRatings", qualifiedByName = "mapTagRatingsToResponse")
    StoreReviewResponse toResponse(StoreReview review);

    @Named("mapImagesToUrls")
    default List<String> mapImagesToUrls(List<ReviewImage> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        return images.stream()
                .filter(img -> !img.isDeleted())
                .map(ReviewImage::getImageUrl)
                .toList();
    }

    @Named("mapTagRatingsToResponse")
    default List<ReviewTagResponse> mapTagRatingsToResponse(List<TagRating> tagRatings) {
        if (tagRatings == null || tagRatings.isEmpty()) {
            return Collections.emptyList();
        }
        return tagRatings.stream()
                .filter(tr -> !tr.isDeleted())
                .map(tr -> ReviewTagResponse.builder()
                        .tagRatingId(tr.getTagRatingId())
                        .tagId(tr.getTag() != null ? tr.getTag().getTagId() : null)
                        .tagName(tr.getTag() != null ? tr.getTag().getName() : null)
                        .categoryName(tr.getTag() != null && tr.getTag().getCategory() != null ? tr.getTag().getCategory().getName() : null)
                        .categoryCode(tr.getTag() != null && tr.getTag().getCategory() != null ? tr.getTag().getCategory().getCode() : null)
                        .score(tr.getScore())
                        .build())
                .toList();
    }
}
