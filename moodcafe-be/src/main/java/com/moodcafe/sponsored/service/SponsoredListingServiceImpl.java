package com.moodcafe.sponsored.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.sponsored.abstraction.repository.SponsoredListingRepository;
import com.moodcafe.sponsored.abstraction.repository.SponsoredPlanRepository;
import com.moodcafe.sponsored.abstraction.service.SponsoredListingService;
import com.moodcafe.sponsored.dto.request.CreateSponsoredListingRequest;
import com.moodcafe.sponsored.dto.request.CreateSponsoredPlanRequest;
import com.moodcafe.sponsored.dto.request.UpdateSponsoredPlanRequest;
import com.moodcafe.sponsored.dto.response.SponsoredAdminStatsResponse;
import com.moodcafe.sponsored.dto.response.SponsoredCheckoutResponse;
import com.moodcafe.sponsored.dto.response.SponsoredListingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredPlacementPriceInfo;
import com.moodcafe.sponsored.dto.response.SponsoredPlanResponse;
import com.moodcafe.sponsored.dto.response.SponsoredPricingResponse;
import com.moodcafe.sponsored.dto.response.SponsoredStoreItemResponse;
import com.moodcafe.sponsored.entity.SponsoredListing;
import com.moodcafe.sponsored.entity.SponsoredPlan;
import com.moodcafe.sponsored.entity.enums.SponsoredDurationType;
import com.moodcafe.sponsored.entity.enums.SponsoredListingStatus;
import com.moodcafe.sponsored.entity.enums.SponsoredPlacement;
import com.moodcafe.sponsored.mapper.SponsoredListingMapper;
import com.moodcafe.sponsored.mapper.SponsoredPlanMapper;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SponsoredListingServiceImpl implements SponsoredListingService {

    private final SponsoredListingRepository sponsoredListingRepository;
    private final SponsoredPlanRepository sponsoredPlanRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StoreStaffService storeStaffService;
    private final SubscriptionService subscriptionService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SponsoredListingMapper sponsoredListingMapper;
    private final SponsoredPlanMapper sponsoredPlanMapper;
    @Lazy
    private final com.moodcafe.payment.service.PayOSService payOSService;

    // =========================================================================
    // MERCHANT & PUBLIC FACING METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SponsoredPlanResponse> getAvailablePlans() {
        return sponsoredPlanRepository.findAllByActiveTrueOrderByPriceAsc()
                .stream()
                .map(sponsoredPlanMapper::toResponse)
                .toList();
    }

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

        List<SponsoredPlanResponse> activePlans = sponsoredPlanRepository.findAllByActiveTrueOrderByPriceAsc()
                .stream()
                .map(sponsoredPlanMapper::toResponse)
                .toList();

        List<SponsoredPlacementPriceInfo> placements = new ArrayList<>();
        for (SponsoredPlacement placement : SponsoredPlacement.values()) {
            boolean recommended = SponsoredPlacement.TOP_BANNER_VIP.equals(placement)
                    || SponsoredPlacement.WEEKEND_PICKS.equals(placement);

            BigDecimal weeklyPrice = activePlans.stream()
                    .filter(p -> placement.equals(p.getPlacement()) && p.getDurationDays() <= 7)
                    .map(SponsoredPlanResponse::getPrice)
                    .findFirst()
                    .orElse(placement.getWeeklyPrice());

            BigDecimal monthlyPrice = activePlans.stream()
                    .filter(p -> placement.equals(p.getPlacement()) && p.getDurationDays() >= 30)
                    .map(SponsoredPlanResponse::getPrice)
                    .findFirst()
                    .orElse(placement.getMonthlyPrice());

            placements.add(SponsoredPlacementPriceInfo.builder()
                    .placement(placement)
                    .displayName(placement.getDisplayName())
                    .description(placement.getDescription())
                    .weeklyPrice(weeklyPrice)
                    .monthlyPrice(monthlyPrice)
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
                .plans(activePlans)
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
        SubscriptionPlan subPlan = activeSub.getSubscriptionPlan();

        if (subPlan == null || !subPlan.isAllowSponsoredListing()) {
            throw new AppException(ErrorCode.SPONSORED_LISTING_NOT_ALLOWED);
        }

        // Lấy gói tài trợ do Admin tạo và quản lý
        SponsoredPlan sponsoredPlan;
        if (request.getPlanId() != null) {
            sponsoredPlan = sponsoredPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_PLAN_NOT_FOUND));
            if (!sponsoredPlan.isActive()) {
                throw new AppException(ErrorCode.SPONSORED_PLAN_INACTIVE);
            }
        } else if (request.getPlacement() != null) {
            // Fallback khi client cũ truyền placement + durationType
            int days = request.getDurationType() != null ? request.getDurationType().getDays() : 7;
            sponsoredPlan = sponsoredPlanRepository.findFirstByPlacementAndDurationDaysAndActiveTrue(request.getPlacement(), days)
                    .orElseGet(() -> sponsoredPlanRepository.findAllByPlacementAndActiveTrueOrderByPriceAsc(request.getPlacement())
                            .stream().findFirst()
                            .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_PLAN_NOT_FOUND)));
        } else {
            throw new AppException(ErrorCode.BAD_REQUEST, "Vui lòng chọn gói tài trợ (planId)");
        }

        Instant startDate = request.getStartDate() != null ? request.getStartDate() : Instant.now();
        Instant endDate = startDate.plus(Duration.ofDays(sponsoredPlan.getDurationDays()));
        SponsoredDurationType durationType = (sponsoredPlan.getDurationDays() >= 30)
                ? SponsoredDurationType.ONE_MONTH
                : SponsoredDurationType.ONE_WEEK;

        String transactionCode = "SPON_PAYOS_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 1. Trường hợp sử dụng lượt miễn phí hàng tháng (Dành riêng cho gói VIP PREMIUM)
        if (request.isUseFreeQuota()) {
            if (!SubscriptionPlanCode.PREMIUM.equals(subPlan.getPlanCode())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Đặc quyền tài trợ miễn phí mỗi tháng chỉ dành cho gói VIP Đặc Quyền (PREMIUM)");
            }
            if (sponsoredPlan.getDurationDays() > 7) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Lượt tài trợ miễn phí chỉ áp dụng cho các gói thời hạn tối đa 7 ngày");
            }
            int usedQuota = activeSub.getMonthlyFreeSponsoredUsed() != null ? activeSub.getMonthlyFreeSponsoredUsed() : 0;
            int maxQuota = subPlan.getMonthlyFreeSponsoredCount() != null ? subPlan.getMonthlyFreeSponsoredCount() : 0;
            if (usedQuota >= maxQuota) {
                throw new AppException(ErrorCode.SPONSORED_FREE_QUOTA_EXCEEDED);
            }

            activeSub.setMonthlyFreeSponsoredUsed(usedQuota + 1);
            userSubscriptionRepository.save(activeSub);

            SponsoredListing campaign = SponsoredListing.builder()
                    .store(store)
                    .user(user)
                    .sponsoredPlan(sponsoredPlan)
                    .placement(sponsoredPlan.getPlacement())
                    .durationType(durationType)
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
            log.info("Free sponsored campaign {} activated for store {} with plan {}",
                    campaign.getSponsoredListingId(), store.getStoreId(), sponsoredPlan.getName());

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

        // 2. Chiến dịch trả phí - Lấy giá trực tiếp từ SponsoredPlan do Admin quản lý
        BigDecimal amount = sponsoredPlan.getPrice();

        SponsoredListing campaign = SponsoredListing.builder()
                .store(store)
                .user(user)
                .sponsoredPlan(sponsoredPlan)
                .placement(sponsoredPlan.getPlacement())
                .durationType(durationType)
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
                .paymentMethod("PAYOS")
                .paymentStatus("PENDING")
                .build();

        campaign = sponsoredListingRepository.save(campaign);

        // Tạo link thanh toán VietQR qua PayOS
        if (payOSService == null || !payOSService.isConfigured()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Cổng thanh toán PayOS chưa được cấu hình trên máy chủ");
        }

        try {
            com.moodcafe.payment.dto.response.PayOSResponse payOSResponse =
                    payOSService.createPaymentLinkForSponsoredListing(campaign);

            String paymentUrl = payOSResponse.getCheckoutUrl();
            String qrCodeUrl = payOSResponse.getQrCode();
            String orderCodeStr = String.valueOf(payOSResponse.getOrderCode());

            return SponsoredCheckoutResponse.builder()
                    .campaignId(campaign.getSponsoredListingId())
                    .transactionCode(orderCodeStr)
                    .storeName(store.getName())
                    .placement(campaign.getPlacement())
                    .placementDisplayName(campaign.getPlacement().getDisplayName())
                    .durationType(campaign.getDurationType())
                    .amount(amount)
                    .freeQuotaUsed(false)
                    .paymentMethod("PAYOS")
                    .paymentUrl(paymentUrl)
                    .qrCodeUrl(qrCodeUrl)
                    .status(SponsoredListingStatus.PENDING)
                    .message("Tạo link thanh toán PayOS VietQR thành công, vui lòng quét mã QR hoặc mở link để thanh toán")
                    .build();
        } catch (Exception e) {
            log.error("Failed to generate PayOS link for sponsored campaign: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.BAD_REQUEST, "Lỗi tạo link thanh toán PayOS: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public SponsoredListingResponse confirmCampaignPayment(String transactionCode) {
        SponsoredListing campaign = sponsoredListingRepository.findByTransactionCode(transactionCode)
                .orElseGet(() -> {
                    try {
                        UUID uuid = UUID.fromString(transactionCode);
                        return sponsoredListingRepository.findById(uuid).orElse(null);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                });

        if (campaign == null) {
            throw new AppException(ErrorCode.SPONSORED_LISTING_NOT_FOUND);
        }

        if (SponsoredListingStatus.ACTIVE.equals(campaign.getStatus())) {
            return sponsoredListingMapper.toResponse(campaign);
        }

        campaign.setStatus(SponsoredListingStatus.ACTIVE);
        campaign.setPaymentStatus("SUCCESS");

        int days = campaign.getSponsoredPlan() != null
                ? campaign.getSponsoredPlan().getDurationDays()
                : (campaign.getDurationType() != null ? campaign.getDurationType().getDays() : 7);

        // Kích hoạt thời hạn bắt đầu từ thời điểm thanh toán thành công
        if (campaign.getStartDate().isBefore(Instant.now())) {
            campaign.setStartDate(Instant.now());
            campaign.setEndDate(Instant.now().plus(Duration.ofDays(days)));
        }

        campaign = sponsoredListingRepository.save(campaign);
        log.info("Campaign {} activated after PayOS payment confirmation", campaign.getSponsoredListingId());

        return sponsoredListingMapper.toResponse(campaign);
    }

    @Override
    @Transactional
    public PageResponse<SponsoredListingResponse> getStoreCampaigns(UUID storeId, String status, Pageable pageable) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");
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

    // =========================================================================
    // ADMIN MANAGEMENT METHODS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SponsoredPlanResponse> getAllPlansAdmin() {
        return sponsoredPlanRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(sponsoredPlanMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SponsoredPlanResponse getPlanByIdAdmin(UUID planId) {
        SponsoredPlan plan = sponsoredPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_PLAN_NOT_FOUND));
        return sponsoredPlanMapper.toResponse(plan);
    }

    @Override
    @Transactional
    public SponsoredPlanResponse createPlan(CreateSponsoredPlanRequest request) {
        if (sponsoredPlanRepository.existsByName(request.getName().trim())) {
            throw new AppException(ErrorCode.SPONSORED_PLAN_NAME_EXISTS);
        }

        SponsoredPlan plan = SponsoredPlan.builder()
                .name(request.getName().trim())
                .displayName(request.getDisplayName().trim())
                .placement(request.getPlacement())
                .durationDays(request.getDurationDays())
                .price(request.getPrice())
                .description(request.getDescription())
                .badgeText(request.getBadgeText())
                .active(request.getActive() == null || request.getActive())
                .build();

        plan = sponsoredPlanRepository.save(plan);
        log.info("Admin created sponsored plan: {} (id: {})", plan.getName(), plan.getSponsoredPlanId());
        return sponsoredPlanMapper.toResponse(plan);
    }

    @Override
    @Transactional
    public SponsoredPlanResponse updatePlan(UUID planId, UpdateSponsoredPlanRequest request) {
        SponsoredPlan plan = sponsoredPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_PLAN_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (sponsoredPlanRepository.existsByNameAndSponsoredPlanIdNot(newName, planId)) {
                throw new AppException(ErrorCode.SPONSORED_PLAN_NAME_EXISTS);
            }
            plan.setName(newName);
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            plan.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getPlacement() != null) {
            plan.setPlacement(request.getPlacement());
        }
        if (request.getDurationDays() != null) {
            plan.setDurationDays(request.getDurationDays());
        }
        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getBadgeText() != null) {
            plan.setBadgeText(request.getBadgeText());
        }
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        plan = sponsoredPlanRepository.save(plan);
        log.info("Admin updated sponsored plan: {} (id: {})", plan.getName(), planId);
        return sponsoredPlanMapper.toResponse(plan);
    }

    @Override
    @Transactional
    public SponsoredPlanResponse togglePlanStatus(UUID planId) {
        SponsoredPlan plan = sponsoredPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_PLAN_NOT_FOUND));

        plan.setActive(!plan.isActive());
        plan = sponsoredPlanRepository.save(plan);
        log.info("Admin toggled sponsored plan {} active status to {}", planId, plan.isActive());
        return sponsoredPlanMapper.toResponse(plan);
    }

    @Override
    @Transactional
    public void deletePlan(UUID planId) {
        SponsoredPlan plan = sponsoredPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SPONSORED_PLAN_NOT_FOUND));

        sponsoredPlanRepository.delete(plan);
        log.info("Admin soft-deleted sponsored plan {}", planId);
    }

    @Override
    @Transactional
    public PageResponse<SponsoredListingResponse> getAllCampaignsAdmin(
            String search,
            SponsoredPlacement placement,
            SponsoredListingStatus status,
            Pageable pageable
    ) {
        updateExpiredCampaigns();

        Page<SponsoredListing> page = sponsoredListingRepository.findAllAdmin(
                (search != null && !search.isBlank()) ? search.trim() : null,
                placement,
                status,
                pageable
        );

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
    @Transactional(readOnly = true)
    public SponsoredAdminStatsResponse getSponsoredStatistics() {
        long total = sponsoredListingRepository.count();
        long active = sponsoredListingRepository.countByStatus(SponsoredListingStatus.ACTIVE);
        long pending = sponsoredListingRepository.countByStatus(SponsoredListingStatus.PENDING);
        long expired = sponsoredListingRepository.countByStatus(SponsoredListingStatus.EXPIRED);
        long cancelled = sponsoredListingRepository.countByStatus(SponsoredListingStatus.CANCELLED);

        BigDecimal totalRevenue = sponsoredListingRepository.sumTotalRevenue();
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal monthlyRevenue = sponsoredListingRepository.sumRevenueSince(startOfMonth);

        Map<String, Long> activeByPlacement = new HashMap<>();
        for (SponsoredPlacement p : SponsoredPlacement.values()) {
            long count = sponsoredListingRepository.countByPlacementAndStatus(p, SponsoredListingStatus.ACTIVE);
            activeByPlacement.put(p.name(), count);
        }

        return SponsoredAdminStatsResponse.builder()
                .totalCampaigns(total)
                .activeCampaigns(active)
                .pendingCampaigns(pending)
                .expiredCampaigns(expired)
                .cancelledCampaigns(cancelled)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .activeCountByPlacement(activeByPlacement)
                .build();
    }

    private void updateExpiredCampaigns() {
        List<SponsoredListing> expired = sponsoredListingRepository
                .findAllByStatusAndEndDateBefore(SponsoredListingStatus.ACTIVE, Instant.now());
        for (SponsoredListing c : expired) {
            c.setStatus(SponsoredListingStatus.EXPIRED);
            sponsoredListingRepository.save(c);
        }
    }
}
