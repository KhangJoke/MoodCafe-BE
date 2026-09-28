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
    @Mapping(target = "verified", expression = "java(review.getVisitVerificationId() != null)")
    @Mapping(target = "reply", expression = "java(mapMerchantReply(review))")
    StoreReviewResponse toResponse(StoreReview review);

    default com.moodcafe.store.dto.response.ReviewReplyResponse mapMerchantReply(StoreReview review) {
        if (review.getMerchantReply() == null || review.getMerchantReply().isBlank()) {
            return null;
        }
        return com.moodcafe.store.dto.response.ReviewReplyResponse.builder()
                .reply(review.getMerchantReply())
                .replyAt(review.getReplyAt())
                .responderName("Phản hồi từ quán")
                .build();
    }

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

    default com.moodcafe.store.dto.response.ReviewReportResponse toReportResponse(com.moodcafe.store.entity.ReviewReport report) {
        if (report == null) return null;
        StoreReview rev = report.getReview();
        return com.moodcafe.store.dto.response.ReviewReportResponse.builder()
                .reportId(report.getReportId())
                .reviewId(rev != null ? rev.getReviewId() : null)
                .reporterUserId(report.getReporter() != null ? report.getReporter().getUserId() : null)
                .reporterFullName(report.getReporter() != null ? report.getReporter().getFullName() : null)
                .storeId(report.getStore() != null ? report.getStore().getStoreId() : (rev != null && rev.getStore() != null ? rev.getStore().getStoreId() : null))
                .storeName(report.getStore() != null ? report.getStore().getName() : (rev != null && rev.getStore() != null ? rev.getStore().getName() : null))
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .reviewContent(rev != null ? rev.getContent() : null)
                .reviewOverallRating(rev != null ? rev.getOverallRating() : null)
                .reviewAuthorFullName(rev != null && rev.getUser() != null ? rev.getUser().getFullName() : null)
                .reviewAuthorUserId(rev != null && rev.getUser() != null ? rev.getUser().getUserId() : null)
                .createdAt(report.getCreatedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}

