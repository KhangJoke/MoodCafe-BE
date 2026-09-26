package com.moodcafe.sponsored.dto.response;

import com.moodcafe.sponsored.entity.enums.SponsoredDurationType;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredListingResponse {

    private UUID sponsoredListingId;
    private UUID storeId;
    private String storeName;
    private String storeAddress;
    private String storePrimaryImageUrl;
    private SponsoredPlacement placement;
    private String placementDisplayName;
    private SponsoredDurationType durationType;
    private Instant startDate;
    private Instant endDate;
    private BigDecimal amount;
    private boolean freeQuotaUsed;
    private SponsoredListingStatus status;
    private Long viewCount;
    private Long clickCount;
    private String customBannerUrl;
    private String title;
    private String transactionCode;
    private String paymentMethod;
    private String paymentStatus;
    private Long remainingDays;
    private Long remainingHours;
    private String remainingText;
    private String badgeLabel;
    private Instant createdAt;
}
