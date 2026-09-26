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
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SponsoredCheckoutResponse {

    private UUID campaignId;
    private String transactionCode;
    private String storeName;
    private SponsoredPlacement placement;
    private String placementDisplayName;
    private SponsoredDurationType durationType;
    private BigDecimal amount;
    private boolean freeQuotaUsed;
    private String paymentMethod;
    private String paymentUrl;
    private String qrCodeUrl;
    private SponsoredListingStatus status;
    private String message;
}
