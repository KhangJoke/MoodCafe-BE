package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.response.StoreReviewResponse;
import com.moodcafe.store.entity.ReviewImage;
import com.moodcafe.store.entity.StoreReview;
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
    StoreReviewResponse toResponse(StoreReview review);

    @Named("mapImagesToUrls")
    default List<String> mapImagesToUrls(List<ReviewImage> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        return images.stream()
                .map(ReviewImage::getImageUrl)
                .toList();
    }
}
