package com.moodcafe.voucher.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.finance.service.PlatformFinanceService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.abstraction.repository.RevenueVoucherCampaignRepository;
import com.moodcafe.voucher.dto.AvailableRevenueVoucherResponse;
import com.moodcafe.voucher.dto.CreateRevenueVoucherCampaignRequest;
import com.moodcafe.voucher.dto.RevenueVoucherCampaignResponse;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.entity.RevenueVoucherCampaign;
import com.moodcafe.voucher.entity.enums.RevenueCampaignStatus;
import com.moodcafe.voucher.entity.enums.RevenueFundingSource;
import com.moodcafe.voucher.entity.enums.RevenueVoucherDistributionType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueVoucherCampaignService {

    public static final String SOURCE_TYPE_REVENUE_GRANT = "REVENUE_GRANT";
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RevenueVoucherCampaignRepository campaignRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final UserRepository userRepository;
    private final PlatformFinanceService platformFinanceService;

    @Transactional
    public RevenueVoucherCampaignResponse createCampaign(CreateRevenueVoucherCampaignRequest request, UUID adminUserId) {
        RevenueFundingSource fundingSource = request.getFundingSource() != null
                ? request.getFundingSource()
                : RevenueFundingSource.ALL;
        BigDecimal faceValue = request.getFaceValueVnd();
        BigDecimal minGross = request.getMinGrossAmount() != null ? request.getMinGrossAmount() : BigDecimal.ZERO;
        int validDays = request.getValidDays() != null ? request.getValidDays() : 30;

        if (request.getStartsAt() != null && request.getEndsAt() != null
                && !request.getEndsAt().isAfter(request.getStartsAt())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Thời gian kết thúc phải sau thời gian bắt đầu");
        }

        if (request.getDistributionType() == RevenueVoucherDistributionType.DIRECT_GRANT) {
            return createDirectGrantCampaign(request, adminUserId, fundingSource, faceValue, minGross, validDays);
        } else {
            return createClaimablePoolCampaign(request, adminUserId, fundingSource, faceValue, minGross, validDays);
        }
    }

    private RevenueVoucherCampaignResponse createDirectGrantCampaign(
            CreateRevenueVoucherCampaignRequest request,
            UUID adminUserId,
            RevenueFundingSource fundingSource,
            BigDecimal faceValue,
            BigDecimal minGross,
            int validDays
    ) {
        Set<UUID> recipientIds = resolveDirectGrantRecipients(request);
        if (recipientIds.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Vui lòng chọn ít nhất 1 người dùng hoặc bật phát cho toàn bộ khách hàng đang hoạt động");
        }

        int totalQty = recipientIds.size();
        BigDecimal requiredBudget = faceValue.multiply(BigDecimal.valueOf(totalQty));
        validateBudgetAvailability(fundingSource, requiredBudget);

        RevenueVoucherCampaign campaign = RevenueVoucherCampaign.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .distributionType(RevenueVoucherDistributionType.DIRECT_GRANT)
                .fundingSource(fundingSource)
                .faceValueVnd(faceValue)
                .minGrossAmount(minGross)
                .validDays(validDays)
                .totalQuantity(totalQty)
                .issuedQuantity(totalQty)
                .maxClaimsPerUser(1)
                .allocatedBudgetVnd(requiredBudget)
                .releasedBudgetVnd(BigDecimal.ZERO)
                .status(RevenueCampaignStatus.COMPLETED)
                .createdBy(adminUserId)
                .build();

        RevenueVoucherCampaign saved = campaignRepository.save(campaign);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(validDays, ChronoUnit.DAYS);
        List<CustomerVoucher> vouchersToSave = new ArrayList<>(totalQty);
        for (UUID recipientId : recipientIds) {
            vouchersToSave.add(CustomerVoucher.builder()
                    .userId(recipientId)
                    .templateId(null)
                    .sourceType(SOURCE_TYPE_REVENUE_GRANT)
                    .campaignId(saved.getCampaignId())
                    .title(saved.getTitle())
                    .code(generateUniqueRevenueVoucherCode())
                    .faceValueVnd(faceValue)
                    .pointCost(0L)
                    .minGrossAmount(minGross)
                    .status("AVAILABLE")
                    .expiresAt(expiresAt)
                    .build());
        }
        customerVoucherRepository.saveAll(vouchersToSave);

        log.info("Admin {} created DIRECT_GRANT revenue voucher campaign {} (source={}, qty={}, budget={} VND)",
                adminUserId, saved.getCampaignId(), fundingSource, totalQty, requiredBudget);

        return toResponse(saved, 0L);
    }

    private RevenueVoucherCampaignResponse createClaimablePoolCampaign(
            CreateRevenueVoucherCampaignRequest request,
            UUID adminUserId,
            RevenueFundingSource fundingSource,
            BigDecimal faceValue,
            BigDecimal minGross,
            int validDays
    ) {
        if (request.getTotalQuantity() == null || request.getTotalQuantity() < 1) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tổng số lượng voucher phải >= 1 cho chiến dịch CLAIMABLE_POOL");
        }
        int totalQty = request.getTotalQuantity();
        int maxPerUser = request.getMaxClaimsPerUser() != null ? request.getMaxClaimsPerUser() : 1;
        BigDecimal requiredBudget = faceValue.multiply(BigDecimal.valueOf(totalQty));
        validateBudgetAvailability(fundingSource, requiredBudget);

        RevenueVoucherCampaign campaign = RevenueVoucherCampaign.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .distributionType(RevenueVoucherDistributionType.CLAIMABLE_POOL)
                .fundingSource(fundingSource)
                .faceValueVnd(faceValue)
                .minGrossAmount(minGross)
                .validDays(validDays)
                .totalQuantity(totalQty)
                .issuedQuantity(0)
                .maxClaimsPerUser(maxPerUser)
                .allocatedBudgetVnd(requiredBudget)
                .releasedBudgetVnd(BigDecimal.ZERO)
                .status(RevenueCampaignStatus.ACTIVE)
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .createdBy(adminUserId)
                .build();

        RevenueVoucherCampaign saved = campaignRepository.save(campaign);
        log.info("Admin {} created CLAIMABLE_POOL revenue voucher campaign {} (source={}, qty={}, budget={} VND)",
                adminUserId, saved.getCampaignId(), fundingSource, totalQty, requiredBudget);

        return toResponse(saved, 0L);
    }

    @Transactional
    public CustomerVoucher claimVoucher(UUID userId, UUID campaignId) {
        if (userId == null || campaignId == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "User ID và Campaign ID không được để trống");
        }

        RevenueVoucherCampaign campaign = campaignRepository.findByIdWithLock(campaignId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chương trình voucher"));

        if (campaign.getDistributionType() != RevenueVoucherDistributionType.CLAIMABLE_POOL) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chương trình này không hỗ trợ tự nhận voucher");
        }

        if (campaign.getStatus() != RevenueCampaignStatus.ACTIVE) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chương trình voucher hiện không còn mở nhận");
        }

        Instant now = Instant.now();
        if (campaign.getStartsAt() != null && now.isBefore(campaign.getStartsAt())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chương trình voucher chưa tới thời gian mở nhận");
        }
        if (campaign.getEndsAt() != null && !now.isBefore(campaign.getEndsAt())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chương trình voucher đã kết thúc thời gian nhận");
        }

        if (campaign.getIssuedQuantity() >= campaign.getTotalQuantity()) {
            campaign.setStatus(RevenueCampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            throw new AppException(ErrorCode.BAD_REQUEST, "Chương trình voucher đã hết lượt nhận");
        }

        long userClaimed = customerVoucherRepository.countByCampaignIdAndUserId(campaignId, userId);
        int maxPerUser = campaign.getMaxClaimsPerUser() != null ? campaign.getMaxClaimsPerUser() : 1;
        if (userClaimed >= maxPerUser) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Bạn đã nhận tối đa " + maxPerUser + " voucher từ chương trình này");
        }

        CustomerVoucher voucher = CustomerVoucher.builder()
                .userId(userId)
                .templateId(null)
                .sourceType(SOURCE_TYPE_REVENUE_GRANT)
                .campaignId(campaign.getCampaignId())
                .title(campaign.getTitle())
                .code(generateUniqueRevenueVoucherCode())
                .faceValueVnd(campaign.getFaceValueVnd())
                .pointCost(0L)
                .minGrossAmount(campaign.getMinGrossAmount())
                .status("AVAILABLE")
                .expiresAt(now.plus(campaign.getValidDays(), ChronoUnit.DAYS))
                .build();

        CustomerVoucher savedVoucher = customerVoucherRepository.save(voucher);

        int newIssued = campaign.getIssuedQuantity() + 1;
        campaign.setIssuedQuantity(newIssued);
        if (newIssued >= campaign.getTotalQuantity()) {
            campaign.setStatus(RevenueCampaignStatus.COMPLETED);
        }
        campaignRepository.save(campaign);

        log.info("User {} claimed free revenue voucher {} (code {}) from campaign {} (0 MoodPoints)",
                userId, savedVoucher.getVoucherId(), savedVoucher.getCode(), campaignId);

        return savedVoucher;
    }

    @Transactional(readOnly = true)
    public List<AvailableRevenueVoucherResponse> getAvailableForCustomer(UUID currentUserId) {
        List<RevenueVoucherCampaign> campaigns = campaignRepository.findAvailableClaimableCampaigns(Instant.now());
        List<AvailableRevenueVoucherResponse> result = new ArrayList<>(campaigns.size());
        for (RevenueVoucherCampaign c : campaigns) {
            long claimed = currentUserId != null
                    ? customerVoucherRepository.countByCampaignIdAndUserId(c.getCampaignId(), currentUserId)
                    : 0L;
            int maxPerUser = c.getMaxClaimsPerUser() != null ? c.getMaxClaimsPerUser() : 1;
            boolean canClaim = c.getRemainingQuantity() > 0 && claimed < maxPerUser;
            result.add(AvailableRevenueVoucherResponse.builder()
                    .campaignId(c.getCampaignId())
                    .title(c.getTitle())
                    .description(c.getDescription())
                    .faceValueVnd(c.getFaceValueVnd())
                    .minGrossAmount(c.getMinGrossAmount())
                    .validDays(c.getValidDays())
                    .totalQuantity(c.getTotalQuantity())
                    .issuedQuantity(c.getIssuedQuantity())
                    .remainingQuantity(c.getRemainingQuantity())
                    .maxClaimsPerUser(maxPerUser)
                    .startsAt(c.getStartsAt())
                    .endsAt(c.getEndsAt())
                    .myClaimedCount(claimed)
                    .canClaim(canClaim)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PageResponse<RevenueVoucherCampaignResponse> listCampaignsAdmin(
            RevenueCampaignStatus status,
            RevenueFundingSource fundingSource,
            RevenueVoucherDistributionType distributionType,
            String search,
            Pageable pageable
    ) {
        Specification<RevenueVoucherCampaign> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fundingSource != null) {
                predicates.add(cb.equal(root.get("fundingSource"), fundingSource));
            }
            if (distributionType != null) {
                predicates.add(cb.equal(root.get("distributionType"), distributionType));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RevenueVoucherCampaign> page = campaignRepository.findAll(spec, pageable);
        List<RevenueVoucherCampaignResponse> items = page.getContent().stream()
                .map(c -> toResponse(c, customerVoucherRepository.countByCampaignIdAndStatus(c.getCampaignId(), "USED")))
                .toList();

        return PageResponse.<RevenueVoucherCampaignResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public RevenueVoucherCampaignResponse getCampaignDetailAdmin(UUID campaignId) {
        RevenueVoucherCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chiến dịch voucher"));
        long usedCount = customerVoucherRepository.countByCampaignIdAndStatus(campaignId, "USED");
        return toResponse(campaign, usedCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerVoucher> getCampaignVouchersAdmin(UUID campaignId, String status, Pageable pageable) {
        if (!campaignRepository.existsById(campaignId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chiến dịch voucher");
        }
        Page<CustomerVoucher> page = (status != null && !status.isBlank())
                ? customerVoucherRepository.findByCampaignIdAndStatusOrderByCreatedAtDesc(campaignId, status.trim().toUpperCase(), pageable)
                : customerVoucherRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable);

        return PageResponse.<CustomerVoucher>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public RevenueVoucherCampaignResponse updateCampaignStatus(UUID campaignId, RevenueCampaignStatus newStatus) {
        RevenueVoucherCampaign campaign = campaignRepository.findByIdWithLock(campaignId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chiến dịch voucher"));

        if (campaign.getStatus() == RevenueCampaignStatus.CANCELLED || campaign.getStatus() == RevenueCampaignStatus.COMPLETED) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Không thể thay đổi trạng thái chiến dịch đã hoàn thành hoặc đã hủy");
        }

        if (newStatus == RevenueCampaignStatus.COMPLETED) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Chỉ hỗ trợ chuyển sang ACTIVE, PAUSED hoặc CANCELLED");
        }

        if (newStatus == RevenueCampaignStatus.CANCELLED) {
            int remaining = campaign.getRemainingQuantity();
            BigDecimal unusedBudget = campaign.getFaceValueVnd().multiply(BigDecimal.valueOf(remaining));
            campaign.setReleasedBudgetVnd(unusedBudget);
            campaign.setStatus(RevenueCampaignStatus.CANCELLED);
            log.info("Cancelled revenue voucher campaign {}: released unused budget {} VND ({} unclaimed vouchers)",
                    campaignId, unusedBudget, remaining);
        } else {
            campaign.setStatus(newStatus);
        }

        RevenueVoucherCampaign saved = campaignRepository.save(campaign);
        long usedCount = customerVoucherRepository.countByCampaignIdAndStatus(campaignId, "USED");
        return toResponse(saved, usedCount);
    }

    private Set<UUID> resolveDirectGrantRecipients(CreateRevenueVoucherCampaignRequest request) {
        Set<UUID> recipientIds = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(request.getGrantToAllActiveCustomers())) {
            List<User> activeCustomers = userRepository.findAllByRoleNameAndActiveTrue("CUSTOMER");
            for (User u : activeCustomers) {
                recipientIds.add(u.getUserId());
            }
        }
        if (request.getTargetUserIds() != null && !request.getTargetUserIds().isEmpty()) {
            for (UUID uid : request.getTargetUserIds()) {
                if (uid != null && userRepository.existsById(uid)) {
                    recipientIds.add(uid);
                }
            }
        }
        return recipientIds;
    }

    private void validateBudgetAvailability(RevenueFundingSource fundingSource, BigDecimal requiredBudget) {
        BigDecimal totalAvailableNetIncome = platformFinanceService.getAvailableNetIncomeAllTime();
        if (requiredBudget.compareTo(totalAvailableNetIncome) > 0) {
            throw new AppException(ErrorCode.BAD_REQUEST,
                    "Ngân sách lợi nhuận ròng (Net Income) không đủ để tạo chiến dịch voucher này (Khả dụng: "
                            + totalAvailableNetIncome.toPlainString() + "đ, Cần: " + requiredBudget.toPlainString() + "đ)");
        }

        if (fundingSource != null && fundingSource != RevenueFundingSource.ALL) {
            BigDecimal sourceAvailable = platformFinanceService.getAvailableBalanceForSource(fundingSource);
            if (requiredBudget.compareTo(sourceAvailable) > 0) {
                String sourceLabel = fundingSource == RevenueFundingSource.SUBSCRIPTION_PAYMENT
                        ? "doanh thu gói dịch vụ (Subscription)"
                        : "doanh thu hoa hồng đặt bàn (Booking Commission)";
                throw new AppException(ErrorCode.BAD_REQUEST,
                        "Số dư khả dụng của nguồn " + sourceLabel + " không đủ (Khả dụng: "
                                + sourceAvailable.toPlainString() + "đ, Cần: " + requiredBudget.toPlainString() + "đ)");
            }
        }
    }

    private RevenueVoucherCampaignResponse toResponse(RevenueVoucherCampaign c, long usedCount) {
        return RevenueVoucherCampaignResponse.builder()
                .campaignId(c.getCampaignId())
                .title(c.getTitle())
                .description(c.getDescription())
                .distributionType(c.getDistributionType())
                .fundingSource(c.getFundingSource())
                .faceValueVnd(c.getFaceValueVnd())
                .minGrossAmount(c.getMinGrossAmount())
                .validDays(c.getValidDays())
                .totalQuantity(c.getTotalQuantity())
                .issuedQuantity(c.getIssuedQuantity())
                .remainingQuantity(c.getRemainingQuantity())
                .usedQuantity(usedCount)
                .maxClaimsPerUser(c.getMaxClaimsPerUser())
                .allocatedBudgetVnd(c.getAllocatedBudgetVnd())
                .releasedBudgetVnd(c.getReleasedBudgetVnd())
                .effectiveExpenseVnd(c.getEffectiveExpenseVnd())
                .status(c.getStatus())
                .startsAt(c.getStartsAt())
                .endsAt(c.getEndsAt())
                .createdBy(c.getCreatedBy())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private String generateUniqueRevenueVoucherCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("MCR-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String candidate = sb.toString();
            if (customerVoucherRepository.findByCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "MCR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

