package com.moodcafe.sponsored.mapper;

import com.moodcafe.sponsored.dto.response.SponsoredListingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredStoreItemResponse;
import com.moodcafe.sponsored.entity.SponsoredListing;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreReviewRepository;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SponsoredListingMapper {

    private final StoreImageRepository storeImageRepository;
    private final StoreReviewRepository storeReviewRepository;

    public SponsoredListingResponse toResponse(SponsoredListing listing) {
        if (listing == null) return null;

        Store store = listing.getStore();
        String primaryImageUrl = getStorePrimaryImageUrl(store);

        Long remainingDays = null;
        Long remainingHours = null;
        String remainingText = "Chờ kích hoạt";

        if (SponsoredListingStatus.ACTIVE.equals(listing.getStatus())) {
            Instant now = Instant.now();
            if (listing.getEndDate() != null && listing.getEndDate().isAfter(now)) {
                Duration duration = Duration.between(now, listing.getEndDate());
                remainingDays = duration.toDays();
                remainingHours = (long) duration.toHoursPart();
                remainingText = "Còn " + remainingDays + " ngày " + remainingHours + " giờ";
            } else {
                remainingText = "Đã hết hạn";
            }
        } else if (SponsoredListingStatus.EXPIRED.equals(listing.getStatus())) {
            remainingText = "Đã hết hạn";
        } else if (SponsoredListingStatus.CANCELLED.equals(listing.getStatus())) {
            remainingText = "Đã hủy";
        }

        return SponsoredListingResponse.builder()
                .sponsoredListingId(listing.getSponsoredListingId())
                .storeId(store != null ? store.getStoreId() : null)
                .storeName(store != null ? store.getName() : null)
                .storeAddress(store != null ? store.getAddress() : null)
                .storePrimaryImageUrl(primaryImageUrl)
                .placement(listing.getPlacement())
                .placementDisplayName(listing.getPlacement() != null ? listing.getPlacement().getDisplayName() : null)
                .durationType(listing.getDurationType())
                .startDate(listing.getStartDate())
                .endDate(listing.getEndDate())
                .amount(listing.getAmount())
                .freeQuotaUsed(listing.isFreeQuotaUsed())
                .status(listing.getStatus())
                .viewCount(listing.getViewCount())
                .clickCount(listing.getClickCount())
                .customBannerUrl(listing.getCustomBannerUrl())
                .title(listing.getTitle())
                .transactionCode(listing.getTransactionCode())
                .paymentMethod(listing.getPaymentMethod())
                .paymentStatus(listing.getPaymentStatus())
                .remainingDays(remainingDays)
                .remainingHours(remainingHours)
                .remainingText(remainingText)
                .badgeLabel("Sponsored")
                .createdAt(listing.getCreatedAt())
                .build();
    }

    public SponsoredStoreItemResponse toStoreItemResponse(SponsoredListing listing) {
        if (listing == null) return null;

        Store store = listing.getStore();
        if (store == null) return null;

        String primaryImageUrl = getStorePrimaryImageUrl(store);

        Double avgRating = 0.0;
        Long reviewCount = 0L;
        List<Object[]> summary = storeReviewRepository.getReviewSummaryByStoreId(store.getStoreId());
        if (summary != null && !summary.isEmpty() && summary.get(0)[5] != null) {
            reviewCount = ((Number) summary.get(0)[5]).longValue();
            if (summary.get(0)[0] != null) {
                avgRating = Math.round(((Number) summary.get(0)[0]).doubleValue() * 10.0) / 10.0;
            }
        }

        return SponsoredStoreItemResponse.builder()
                .campaignId(listing.getSponsoredListingId())
                .storeId(store.getStoreId())
                .name(store.getName())
                .address(store.getAddress())
                .coverImageUrl(primaryImageUrl)
                .customBannerUrl(listing.getCustomBannerUrl())
                .campaignTitle(listing.getTitle())
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .priceFrom(store.getPriceFrom())
                .priceTo(store.getPriceTo())
                .sponsored(true)
                .sponsoredPlacement(listing.getPlacement())
                .badgeText("Sponsored")
                .build();
    }

    private String getStorePrimaryImageUrl(Store store) {
        if (store == null) return null;
        return storeImageRepository.findByStoreStoreIdAndPrimaryTrue(store.getStoreId())
                .map(StoreImage::getImageUrl)
                .orElseGet(() -> storeImageRepository.findAllByStoreStoreId(store.getStoreId())
                        .stream().findFirst().map(StoreImage::getImageUrl).orElse(null));
    }
}
