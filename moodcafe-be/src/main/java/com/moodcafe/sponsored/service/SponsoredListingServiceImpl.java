package com.moodcafe.sponsored.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.sponsored.abstraction.repository.SponsoredListingRepository;
import com.moodcafe.sponsored.abstraction.service.SponsoredListingService;
import com.moodcafe.sponsored.dto.request.CreateSponsoredListingRequest;
import com.moodcafe.sponsored.dto.response.SponsoredCheckoutResponse;
import com.moodcafe.sponsored.dto.response.SponsoredListingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredPlacementPriceInfo;
import com.moodcafe.sponsored.dto.response.SponsoredPricingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredStoreItemResponse;
import com.moodcafe.sponsored.entity.SponsoredListing;
import com.moodcafe.sponsored.entity.enums.SponsoredDurationType;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import com.moodcafe.sponsored.mapper.SponsoredListingMapper;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import com.moodcafe.subscription.abstraction.repository.UserSubscriptionRepository;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.dto.response.UserSubscriptionResponse;
import com.moodcafe.subscription.entity.SubscriptionPlan;
import com.moodcafe.subscription.entity.UserSubscription;
import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SponsoredListingServiceImpl implements SponsoredListingService {

    private final SponsoredListingRepository sponsoredListingRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreStaffService storeStaffService;
    private final SubscriptionService subscriptionService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SponsoredListingMapper sponsoredListingMapper;

    @Override
    @Transactional(readOnly = true)
    public SponsoredPricingResponse getPricingAndEligibility(UUID userId, UUID storeId) {
        UserSubscriptionResponse currentSub = subscriptionService.getCurrentUserSubscription(userId);

        boolean isEligible = currentSub.getPlan() != null && currentSub.getPlan().isAllowSponsoredListing();
        String planCode = currentSub.getPlan() != null ? currentSub.getPlan().getPlanCode().name() : "BASIC";
        String planName = currentSub.getPlan() != null ? currentSub.getPlan().getDisplayName() : "Cơ Bản (Basic)";

        String warningBanner = null;
        if (!isEligible) {
            warningBanner = "Tài khoản của bạn hiện ở gói Basic. Vui lòng nâng cấp lên gói PRO hoặc PREMIUM để mở khóa tương tác đăng ký các vị trí quảng bá trên.";
        }

        boolean freeQuotaAvailable = currentSub.getMonthlyFreeSponsoredRemaining() != null
                && currentSub.getMonthlyFreeSponsoredRemaining() > 0;
        int freeQuotaRemaining = currentSub.getMonthlyFreeSponsoredRemaining() != null
                ? currentSub.getMonthlyFreeSponsoredRemaining()
                : 0;

        List<SponsoredPlacementPriceInfo> placements = new ArrayList<>();
        for (SponsoredPlacement placement : SponsoredPlacement.values()) {
            boolean recommended = SponsoredPlacement.TOP_BANNER_VIP.equals(placement)
                    || SponsoredPlacement.WEEKEND_PICKS.equals(placement);

            placements.add(SponsoredPlacementPriceInfo.builder()
                    .placement(placement)
                    .displayName(placement.getDisplayName())
                    .description(placement.getDescription())
                    .weeklyPrice(placement.getWeeklyPrice())
                    .monthlyPrice(placement.getMonthlyPrice())
                    .monthlySavingsPercent(20)
                    .recommended(recommended)
                    .build());
        }

        return SponsoredPricingResponse.builder()
                .eligible(isEligible)
                .userPlanCode(planCode)
                .userPlanName(planName)
                .warningBannerMessage(warningBanner)
                .freeQuotaAvailable(freeQuotaAvailable)
                .freeQuotaRemaining(freeQuotaRemaining)
                .placements(placements)
                .build();
    }

    @Override
    @Transactional
    public SponsoredCheckoutResponse registerCampaign(UUID userId, CreateSponsoredListingRequest request) {
        storeStaffService.requireStoreAccess(request.getStoreId(), "OWNER", "MANAGER");

        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserSubscription activeSub = subscriptionService.getActiveUserSubscriptionEntity(userId);
        SubscriptionPlan plan = activeSub.getSubscriptionPlan();

        if (plan == null || !plan.isAllowSponsoredListing()) {
            throw new AppException(ErrorCode.SPONSORED_LISTING_NOT_ALLOWED);
        }

        Instant startDate = request.getStartDate() != null ? request.getStartDate() : Instant.now();
        Instant endDate = startDate.plus(Duration.ofDays(request.getDurationType().getDays()));

        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "VNPAY";
        String transactionCode = "SPON_" + paymentMethod + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // Check if merchant requested to use monthly free quota
        if (request.isUseFreeQuota()) {
            if (!SubscriptionPlanCode.PREMIUM.equals(plan.getPlanCode())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Đặc quyền tài trợ miễn phí mỗi tháng chỉ dành cho gói VIP Đặc Quyền (PREMIUM)");
            }
            if (!SponsoredDurationType.ONE_WEEK.equals(request.getDurationType())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Lượt tài trợ miễn phí chỉ áp dụng cho thời hạn 1 tuần");
            }
            int usedQuota = activeSub.getMonthlyFreeSponsoredUsed() != null ? activeSub.getMonthlyFreeSponsoredUsed() : 0;
            int maxQuota = plan.getMonthlyFreeSponsoredCount() != null ? plan.getMonthlyFreeSponsoredCount() : 0;
            if (usedQuota >= maxQuota) {
                throw new AppException(ErrorCode.SPONSORED_FREE_QUOTA_EXCEEDED);
            }

            activeSub.setMonthlyFreeSponsoredUsed(usedQuota + 1);
            userSubscriptionRepository.save(activeSub);

            SponsoredListing campaign = SponsoredListing.builder()
                    .store(store)
                    .user(user)
                    .placement(request.getPlacement())
                    .durationType(request.getDurationType())
                    .startDate(startDate)
                    .endDate(endDate)
                    .amount(BigDecimal.ZERO)
                    .freeQuotaUsed(true)
                    .status(SponsoredListingStatus.ACTIVE)
                    .viewCount(0L)
                    .clickCount(0L)
                    .customBannerUrl(request.getCustomBannerUrl())
                    .title(request.getTitle())
                    .transactionCode(transactionCode)
                    .paymentMethod("FREE_QUOTA")
                    .paymentStatus("SUCCESS")
                    .build();

            campaign = sponsoredListingRepository.save(campaign);
            log.info("Free sponsored campaign {} activated for store {}", campaign.getSponsoredListingId(), store.getStoreId());

            return SponsoredCheckoutResponse.builder()
                    .campaignId(campaign.getSponsoredListingId())
                    .transactionCode(transactionCode)
                    .storeName(store.getName())
                    .placement(campaign.getPlacement())
                    .placementDisplayName(campaign.getPlacement().getDisplayName())
                    .durationType(campaign.getDurationType())
                    .amount(BigDecimal.ZERO)
                    .freeQuotaUsed(true)
                    .paymentMethod("FREE_QUOTA")
                    .status(SponsoredListingStatus.ACTIVE)
                    .message("Kích hoạt chiến dịch quảng bá miễn phí 7 ngày thành công!")
                    .build();
        }

        // Paid campaign
        BigDecimal amount = request.getPlacement().getPriceByDuration(request.getDurationType());

        SponsoredListing campaign = SponsoredListing.builder()
                .store(store)
                .user(user)
                .placement(request.getPlacement())
                .durationType(request.getDurationType())
                .startDate(startDate)
                .endDate(endDate)
                .amount(amount)
                .freeQuotaUsed(false)
                .status(SponsoredListingStatus.PENDING)
                .viewCount(0L)
                .clickCount(0L)
                .customBannerUrl(request.getCustomBannerUrl())
                .title(request.getTitle())
                .transactionCode(transactionCode)
                .paymentMethod(paymentMethod)
                .paymentStatus("PENDING")
                .build();

        campaign = sponsoredListingRepository.save(campaign);

        String paymentUrl = buildMockPaymentUrl(transactionCode, amount, paymentMethod);

        return SponsoredCheckoutResponse.builder()
                .campaignId(campaign.getSponsoredListingId())
                .transactionCode(transactionCode)
                .storeName(store.getName())
                .placement(campaign.getPlacement())
                .placementDisplayName(campaign.getPlacement().getDisplayName())
                .durationType(campaign.getDurationType())
                .amount(amount)
                .freeQuotaUsed(false)
                .paymentMethod(paymentMethod)
                .paymentUrl(paymentUrl)
                .qrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + paymentUrl)
                .status(SponsoredListingStatus.PENDING)
                .message("Tạo đơn tài trợ thành công, vui lòng tiến hành thanh toán")
                .build();
    }

    @Override
    @Transactional
    public SponsoredListingResponse confirmCampaignPayment(String transactionCode) {
        SponsoredListing campaign = sponsoredListingRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_LISTING_NOT_FOUND));

        if (SponsoredListingStatus.ACTIVE.equals(campaign.getStatus())) {
            return sponsoredListingMapper.toResponse(campaign);
        }

        campaign.setStatus(SponsoredListingStatus.ACTIVE);
        campaign.setPaymentStatus("SUCCESS");
        // Recalculate end date based on current activation time if requested start date was in the past
        if (campaign.getStartDate().isBefore(Instant.now())) {
            campaign.setStartDate(Instant.now());
            campaign.setEndDate(Instant.now().plus(Duration.ofDays(campaign.getDurationType().getDays())));
        }

        campaign = sponsoredListingRepository.save(campaign);
        log.info("Campaign {} activated after payment confirmation", campaign.getSponsoredListingId());

        return sponsoredListingMapper.toResponse(campaign);
    }

    @Override
    @Transactional
    public PageResponse<SponsoredListingResponse> getStoreCampaigns(UUID storeId, String status, Pageable pageable) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        // Auto update expired campaigns
        updateExpiredCampaigns();

        Page<SponsoredListing> page;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            try {
                SponsoredListingStatus statusEnum = SponsoredListingStatus.valueOf(status.toUpperCase());
                page = sponsoredListingRepository.findAllByStoreStoreIdAndStatusOrderByCreatedAtDesc(storeId, statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                page = sponsoredListingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId, pageable);
            }
        } else {
            page = sponsoredListingRepository.findAllByStoreStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }

        List<SponsoredListingResponse> items = page.getContent()
                .stream()
                .map(sponsoredListingMapper::toResponse)
                .toList();

        return PageResponse.<SponsoredListingResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public SponsoredListingResponse cancelCampaign(UUID storeId, UUID campaignId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        SponsoredListing campaign = sponsoredListingRepository.findById(campaignId)
                .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_LISTING_NOT_FOUND));

        if (!campaign.getStore().getStoreId().equals(storeId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chiến dịch không thuộc về quán này");
        }

        if (SponsoredListingStatus.EXPIRED.equals(campaign.getStatus()) || SponsoredListingStatus.CANCELLED.equals(campaign.getStatus())) {
            throw new AppException(ErrorCode.SPONSORED_LISTING_CANNOT_CANCEL);
        }

        campaign.setStatus(SponsoredListingStatus.CANCELLED);
        campaign = sponsoredListingRepository.save(campaign);
        log.info("Campaign {} for store {} has been cancelled", campaignId, storeId);

        return sponsoredListingMapper.toResponse(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SponsoredStoreItemResponse> getActiveStoresByPlacement(SponsoredPlacement placement) {
        List<SponsoredListing> activeCampaigns = sponsoredListingRepository
                .findActiveByPlacement(placement, SponsoredListingStatus.ACTIVE, Instant.now());

        return activeCampaigns.stream()
                .map(sponsoredListingMapper::toStoreItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public void recordImpression(UUID campaignId) {
        sponsoredListingRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setViewCount(campaign.getViewCount() + 1);
            sponsoredListingRepository.save(campaign);
        });
    }

    @Override
    @Transactional
    public void recordClick(UUID campaignId) {
        sponsoredListingRepository.findById(campaignId).ifPresent(campaign -> {
            campaign.setClickCount(campaign.getClickCount() + 1);
            sponsoredListingRepository.save(campaign);
        });
    }

    private void updateExpiredCampaigns() {
        List<SponsoredListing> expired = sponsoredListingRepository
                .findAllByStatusAndEndDateBefore(SponsoredListingStatus.ACTIVE, Instant.now());
        for (SponsoredListing c : expired) {
            c.setStatus(SponsoredListingStatus.EXPIRED);
            sponsoredListingRepository.save(c);
        }
    }

    private String buildMockPaymentUrl(String txnRef, BigDecimal amount, String method) {
        String baseUrl = "VNPAY".equalsIgnoreCase(method)
                ? "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                : "https://test-payment.momo.vn/v2/gateway/api/create";
        return baseUrl + "?vnp_TxnRef=" + txnRef + "&vnp_Amount=" + amount.toBigInteger().multiply(java.math.BigInteger.valueOf(100))
                + "&vnp_OrderInfo=" + java.net.URLEncoder.encode("Thanh toan MoodCafe Sponsored " + txnRef, java.nio.charset.StandardCharsets.UTF_8);
    }
}
