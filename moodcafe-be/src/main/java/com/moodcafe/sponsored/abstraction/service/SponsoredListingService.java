package com.moodcafe.sponsored.abstraction.service;

import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.sponsored.dto.request.CreateSponsoredListingRequest;
import com.moodcafe.sponsored.dto.response.SponsoredCheckoutResponse;
import com.moodcafe.sponsored.dto.response.SponsoredListingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredPricingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredStoreItemResponse;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SponsoredListingService {

    SponsoredPricingResponse getPricingAndEligibility(UUID userId, UUID storeId);

    SponsoredCheckoutResponse registerCampaign(UUID userId, CreateSponsoredListingRequest request);

    SponsoredListingResponse confirmCampaignPayment(String transactionCode);

    PageResponse<SponsoredListingResponse> getStoreCampaigns(UUID storeId, String status, Pageable pageable);

    SponsoredListingResponse cancelCampaign(UUID storeId, UUID campaignId);

    List<SponsoredStoreItemResponse> getActiveStoresByPlacement(SponsoredPlacement placement);

    void recordImpression(UUID campaignId);

    void recordClick(UUID campaignId);
}
