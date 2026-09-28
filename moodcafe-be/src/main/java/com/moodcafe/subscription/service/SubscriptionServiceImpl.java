package com.moodcafe.subscription.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPaymentRepository;
import com.moodcafe.subscription.abstraction.repository.SubscriptionPlanRepository;
import com.moodcafe.subscription.abstraction.repository.UserSubscriptionRepository;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.dto.request.CreateSubscriptionPlanRequest;
import com.moodcafe.subscription.dto.request.SubscribePlanRequest;
import com.moodcafe.subscription.dto.request.UpdateSubscriptionPlanRequest;
import com.moodcafe.subscription.dto.response.PlanSubscriptionStatsResponse;
import com.moodcafe.subscription.dto.response.SubscriptionCheckoutResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPaymentResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPlanResponse;
import com.moodcafe.subscription.dto.response.SubscriptionStatisticsResponse;
import com.moodcafe.subscription.dto.response.UserSubscriptionResponse;
import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.SubscriptionPlan;
import com.moodcafe.subscription.entity.UserSubscription;
import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import com.moodcafe.subscription.entity.enums.SubscriptionStatus;
import com.moodcafe.subscription.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final UserRepository userRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final SubscriptionMapper subscriptionMapper;
    @org.springframework.context.annotation.Lazy
    private final com.moodcafe.payment.service.PayOSService payOSService;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAvailablePlans() {
        return subscriptionPlanRepository.findAllByActiveTrueOrderByPriceAsc()
                .stream()
                .map(subscriptionMapper::toPlanResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserSubscriptionResponse getCurrentUserSubscription(UUID userId) {
        UserSubscription activeSub = getActiveUserSubscriptionEntity(userId);
        int ownedBranchCount = (int) storeStaffRepository.countByUserUserIdAndStoreRoleName(userId, "OWNER");
        return subscriptionMapper.toUserSubscriptionResponse(activeSub, ownedBranchCount);
    }

    @Override
    @Transactional
    public UserSubscription getActiveUserSubscriptionEntity(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Optional<UserSubscription> subOpt = userSubscriptionRepository
                .findFirstByUserUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE);

        if (subOpt.isPresent()) {
            UserSubscription sub = subOpt.get();
            // Check if subscription has expired
            if (sub.getEndDate() != null && sub.getEndDate().isBefore(Instant.now())) {
                log.info("Subscription {} for user {} has expired", sub.getUserSubscriptionId(), userId);
                sub.setStatus(SubscriptionStatus.EXPIRED);
                userSubscriptionRepository.save(sub);
                return provisionDefaultBasicSubscription(user);
            }
            return sub;
        }

        // If no active subscription exists, provision the default BASIC plan
        return provisionDefaultBasicSubscription(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPaymentResponse> getPaymentHistory(UUID userId, Pageable pageable) {
        Page<SubscriptionPayment> page = subscriptionPaymentRepository
                .findAllByUserUserIdOrderByCreatedAtDesc(userId, pageable);

        List<SubscriptionPaymentResponse> items = page.getContent()
                .stream()
                .map(subscriptionMapper::toPaymentResponse)
                .toList();

        return PageResponse.<SubscriptionPaymentResponse>builder()
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
    public SubscriptionCheckoutResponse subscribePlan(UUID userId, SubscribePlanRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCode(request.getPlanCode())
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

        int durationMonths = request.getDurationMonths() != null && request.getDurationMonths() > 0
                ? request.getDurationMonths()
                : 1;

        BigDecimal totalAmount = plan.getPrice().multiply(BigDecimal.valueOf(durationMonths));
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "VNPAY";

        String transactionCode = "SUB_" + paymentMethod + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        // If free plan (BASIC), activate immediately
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            cancelExistingActiveSubscriptions(userId);

            UserSubscription newSub = UserSubscription.builder()
                    .user(user)
                    .subscriptionPlan(plan)
                    .startDate(Instant.now())
                    .endDate(null) // Lifetime for Basic
                    .status(SubscriptionStatus.ACTIVE)
                    .monthlyFreeSponsoredUsed(0)
                    .autoRenew(false)
                    .build();
            userSubscriptionRepository.save(newSub);

            SubscriptionPayment payment = SubscriptionPayment.builder()
                    .user(user)
                    .subscription(newSub)
                    .subscriptionPlan(plan)
                    .transactionCode(transactionCode)
                    .amount(BigDecimal.ZERO)
                    .paymentMethod("FREE")
                    .status(SubscriptionPaymentStatus.SUCCESS)
                    .paidAt(Instant.now())
                    .notes("Đăng ký gói Cơ Bản miễn phí")
                    .build();
            subscriptionPaymentRepository.save(payment);

            return SubscriptionCheckoutResponse.builder()
                    .paymentId(payment.getPaymentId())
                    .transactionCode(transactionCode)
                    .planCode(plan.getPlanCode())
                    .planDisplayName(plan.getDisplayName())
                    .amount(BigDecimal.ZERO)
                    .paymentMethod("FREE")
                    .status(SubscriptionPaymentStatus.SUCCESS)
                    .message("Kích hoạt gói Cơ Bản thành công")
                    .build();
        }

        // Paid plan (PRO or PREMIUM)
        SubscriptionPayment payment = SubscriptionPayment.builder()
                .user(user)
                .subscriptionPlan(plan)
                .transactionCode(transactionCode)
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .status(SubscriptionPaymentStatus.PENDING)
                .notes("Đăng ký gói " + plan.getDisplayName() + " (" + durationMonths + " tháng)")
                .build();

        String paymentUrl;
        String qrCodeUrl;
        String message = "Tạo đơn đăng ký gói thành công, vui lòng tiến hành thanh toán";

        if ("PAYOS".equalsIgnoreCase(paymentMethod) && payOSService != null && payOSService.isConfigured()) {
            payment = subscriptionPaymentRepository.save(payment);
            try {
                com.moodcafe.payment.dto.response.PayOSResponse payOSResponse =
                        payOSService.createPaymentLinkForSubscriptionPayment(payment);
                paymentUrl = payOSResponse.getCheckoutUrl();
                qrCodeUrl = payOSResponse.getQrCode();
                transactionCode = String.valueOf(payOSResponse.getOrderCode());
                message = "Tạo link thanh toán PayOS VietQR thành công, vui lòng quét mã QR hoặc mở link để thanh toán";
            } catch (Exception e) {
                log.error("Failed to generate PayOS link, falling back to standard mock URL: {}", e.getMessage());
                paymentUrl = buildMockPaymentUrl(transactionCode, totalAmount, paymentMethod);
                qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + paymentUrl;
                payment.setPaymentUrl(paymentUrl);
                payment = subscriptionPaymentRepository.save(payment);
            }
        } else {
            paymentUrl = buildMockPaymentUrl(transactionCode, totalAmount, paymentMethod);
            qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + paymentUrl;
            payment.setPaymentUrl(paymentUrl);
            payment = subscriptionPaymentRepository.save(payment);
        }

        return SubscriptionCheckoutResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionCode(transactionCode)
                .planCode(plan.getPlanCode())
                .planDisplayName(plan.getDisplayName())
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .status(SubscriptionPaymentStatus.PENDING)
                .paymentUrl(paymentUrl)
                .qrCodeUrl(qrCodeUrl)
                .message(message)
                .build();
    }

    @Override
    @Transactional
    public UserSubscriptionResponse confirmPayment(String transactionCode) {
        SubscriptionPayment payment = subscriptionPaymentRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PAYMENT_NOT_FOUND));

        if (SubscriptionPaymentStatus.SUCCESS.equals(payment.getStatus())) {
            int branchCount = (int) storeStaffRepository.countByUserUserIdAndStoreRoleName(payment.getUser().getUserId(), "OWNER");
            return subscriptionMapper.toUserSubscriptionResponse(payment.getSubscription(), branchCount);
        }

        SubscriptionPlan plan = payment.getSubscriptionPlan();
        if (plan == null) {
            throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND);
        }

        // Update payment to SUCCESS
        payment.setStatus(SubscriptionPaymentStatus.SUCCESS);
        payment.setPaidAt(Instant.now());

        // Cancel previous active subscriptions
        cancelExistingActiveSubscriptions(payment.getUser().getUserId());

        // Determine duration days (default 30 days per plan price)
        long durationDays = plan.getDurationDays() != null && plan.getDurationDays() > 0 ? plan.getDurationDays() : 30L;
        // If amount was for multiple months
        if (plan.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            long months = payment.getAmount().divideToIntegralValue(plan.getPrice()).longValue();
            if (months > 1) {
                durationDays = 30L * months;
            }
        }

        Instant startDate = Instant.now();
        Instant endDate = startDate.plus(Duration.ofDays(durationDays));

        UserSubscription sub = UserSubscription.builder()
                .user(payment.getUser())
                .subscriptionPlan(plan)
                .startDate(startDate)
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .monthlyFreeSponsoredUsed(0)
                .autoRenew(true)
                .build();

        sub = userSubscriptionRepository.save(sub);
        payment.setSubscription(sub);
        subscriptionPaymentRepository.save(payment);

        log.info("Subscription activated for user {} with plan {}", payment.getUser().getUserId(), plan.getPlanCode());

        int branchCount = (int) storeStaffRepository.countByUserUserIdAndStoreRoleName(payment.getUser().getUserId(), "OWNER");
        return subscriptionMapper.toUserSubscriptionResponse(sub, branchCount);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBranchLimit(UUID userId) {
        UserSubscription activeSub = getActiveUserSubscriptionEntity(userId);
        SubscriptionPlan plan = activeSub.getSubscriptionPlan();
        if (plan == null) return;

        int maxBranches = plan.getMaxBranches();
        if (maxBranches == -1) {
            return; // Unlimited branches for PREMIUM
        }

        long ownedCount = storeStaffRepository.countByUserUserIdAndStoreRoleName(userId, "OWNER");
        if (ownedCount >= maxBranches) {
            throw new AppException(ErrorCode.MAX_BRANCH_LIMIT_EXCEEDED,
                    "Gói dịch vụ " + plan.getDisplayName() + " chỉ cho phép quản lý tối đa "
                            + maxBranches + " chi nhánh. Hiện bạn đã sở hữu " + ownedCount
                            + " chi nhánh. Vui lòng nâng cấp lên gói PRO hoặc PREMIUM để tiếp tục mở rộng.");
        }
    }

    private UserSubscription provisionDefaultBasicSubscription(User user) {
        SubscriptionPlan basicPlan = subscriptionPlanRepository.findByPlanCode(SubscriptionPlanCode.BASIC)
                .orElseGet(() -> subscriptionPlanRepository.findByName("BASIC")
                        .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND, "BASIC plan not configured")));

        UserSubscription defaultSub = UserSubscription.builder()
                .user(user)
                .subscriptionPlan(basicPlan)
                .startDate(Instant.now())
                .endDate(null)
                .status(SubscriptionStatus.ACTIVE)
                .monthlyFreeSponsoredUsed(0)
                .autoRenew(false)
                .build();

        return userSubscriptionRepository.save(defaultSub);
    }

    private void cancelExistingActiveSubscriptions(UUID userId) {
        List<UserSubscription> existing = userSubscriptionRepository
                .findAllByUserUserIdOrderByCreatedAtDesc(userId);
        for (UserSubscription sub : existing) {
            if (SubscriptionStatus.ACTIVE.equals(sub.getStatus())) {
                sub.setStatus(SubscriptionStatus.CANCELLED);
                userSubscriptionRepository.save(sub);
            }
        }
    }

    private String buildMockPaymentUrl(String txnRef, BigDecimal amount, String method) {
        String baseUrl = "VNPAY".equalsIgnoreCase(method)
                ? "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                : "https://test-payment.momo.vn/v2/gateway/api/create";
        return baseUrl + "?vnp_TxnRef=" + txnRef + "&vnp_Amount=" + amount.toBigInteger().multiply(java.math.BigInteger.valueOf(100))
                + "&vnp_OrderInfo=" + java.net.URLEncoder.encode("Thanh toan MoodCafe Subscription " + txnRef, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlansAdmin() {
        return subscriptionPlanRepository.findAllByOrderByPriceAsc()
                .stream()
                .map(subscriptionMapper::toPlanResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlanByIdAdmin(UUID planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));
        return subscriptionMapper.toPlanResponse(plan);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request) {
        if (subscriptionPlanRepository.existsByName(request.getName().trim())) {
            throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_NAME_EXISTS);
        }
        if (request.getPlanCode() != null && subscriptionPlanRepository.findByPlanCode(request.getPlanCode()).isPresent()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Plan code " + request.getPlanCode() + " đã tồn tại");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName().trim())
                .planCode(request.getPlanCode())
                .displayName(request.getDisplayName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationDays(request.getDurationDays() != null ? request.getDurationDays() : 30)
                .maxBranches(request.getMaxBranches() != null ? request.getMaxBranches() : 1)
                .tableManagement(Boolean.TRUE.equals(request.getTableManagement()))
                .advancedAnalytics(Boolean.TRUE.equals(request.getAdvancedAnalytics()))
                .depositRules(Boolean.TRUE.equals(request.getDepositRules()))
                .allowSponsoredListing(Boolean.TRUE.equals(request.getAllowSponsoredListing()))
                .vipHeroBanner(Boolean.TRUE.equals(request.getVipHeroBanner()))
                .qrTableMenu(Boolean.TRUE.equals(request.getQrTableMenu()))
                .aiRecommendation(Boolean.TRUE.equals(request.getAiRecommendation()))
                .monthlyFreeSponsoredCount(request.getMonthlyFreeSponsoredCount() != null ? request.getMonthlyFreeSponsoredCount() : 0)
                .dedicatedSupport(Boolean.TRUE.equals(request.getDedicatedSupport()))
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        plan = subscriptionPlanRepository.save(plan);
        log.info("Admin created subscription plan: {} ({})", plan.getName(), plan.getSubscriptionPlanId());
        return subscriptionMapper.toPlanResponse(plan);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse updatePlan(UUID planId, UpdateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            if (subscriptionPlanRepository.existsByNameAndSubscriptionPlanIdNot(request.getName().trim(), planId)) {
                throw new AppException(ErrorCode.SUBSCRIPTION_PLAN_NAME_EXISTS);
            }
            plan.setName(request.getName().trim());
        }
        if (request.getPlanCode() != null) {
            if (subscriptionPlanRepository.existsByPlanCodeAndSubscriptionPlanIdNot(request.getPlanCode(), planId)) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Plan code " + request.getPlanCode() + " đã tồn tại");
            }
            plan.setPlanCode(request.getPlanCode());
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            plan.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getDescription() != null) {
            plan.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            plan.setPrice(request.getPrice());
        }
        if (request.getDurationDays() != null) {
            plan.setDurationDays(request.getDurationDays());
        }
        if (request.getMaxBranches() != null) {
            plan.setMaxBranches(request.getMaxBranches());
        }
        if (request.getTableManagement() != null) {
            plan.setTableManagement(request.getTableManagement());
        }
        if (request.getAdvancedAnalytics() != null) {
            plan.setAdvancedAnalytics(request.getAdvancedAnalytics());
        }
        if (request.getDepositRules() != null) {
            plan.setDepositRules(request.getDepositRules());
        }
        if (request.getAllowSponsoredListing() != null) {
            plan.setAllowSponsoredListing(request.getAllowSponsoredListing());
        }
        if (request.getVipHeroBanner() != null) {
            plan.setVipHeroBanner(request.getVipHeroBanner());
        }
        if (request.getQrTableMenu() != null) {
            plan.setQrTableMenu(request.getQrTableMenu());
        }
        if (request.getAiRecommendation() != null) {
            plan.setAiRecommendation(request.getAiRecommendation());
        }
        if (request.getMonthlyFreeSponsoredCount() != null) {
            plan.setMonthlyFreeSponsoredCount(request.getMonthlyFreeSponsoredCount());
        }
        if (request.getDedicatedSupport() != null) {
            plan.setDedicatedSupport(request.getDedicatedSupport());
        }
        if (request.getActive() != null) {
            plan.setActive(request.getActive());
        }

        plan = subscriptionPlanRepository.save(plan);
        log.info("Admin updated subscription plan: {}", plan.getSubscriptionPlanId());
        return subscriptionMapper.toPlanResponse(plan);
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse togglePlanStatus(UUID planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

        plan.setActive(!plan.isActive());
        plan = subscriptionPlanRepository.save(plan);
        log.info("Admin toggled subscription plan {} active status to {}", planId, plan.isActive());
        return subscriptionMapper.toPlanResponse(plan);
    }

    @Override
    @Transactional
    public void deletePlan(UUID planId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.SUBSCRIPTION_PLAN_NOT_FOUND));

        subscriptionPlanRepository.delete(plan);
        log.info("Admin soft-deleted subscription plan {}", planId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPaymentResponse> getAllPaymentsAdmin(Pageable pageable) {
        return getAllPaymentsAdmin(null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPaymentResponse> getAllPaymentsAdmin(
            String search,
            SubscriptionPaymentStatus status,
            UUID planId,
            Pageable pageable
    ) {
        Specification<SubscriptionPayment> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (planId != null) {
                predicates.add(cb.equal(root.get("subscriptionPlan").get("subscriptionPlanId"), planId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate codeMatch = cb.like(cb.lower(root.get("transactionCode")), pattern);
                jakarta.persistence.criteria.Predicate nameMatch = cb.like(cb.lower(root.get("user").get("fullName")), pattern);
                jakarta.persistence.criteria.Predicate emailMatch = cb.like(cb.lower(root.get("user").get("email")), pattern);
                predicates.add(cb.or(codeMatch, nameMatch, emailMatch));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<SubscriptionPayment> page = subscriptionPaymentRepository.findAll(spec, pageable);
        return PageResponse.<SubscriptionPaymentResponse>builder()
                .items(page.getContent().stream().map(subscriptionMapper::toPaymentResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPaymentResponse> getOwnerPaymentHistoryAdmin(UUID ownerUserId, Pageable pageable) {
        return getPaymentHistory(ownerUserId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSubscriptionResponse> getAllUserSubscriptionsAdmin(Pageable pageable) {
        return getAllUserSubscriptionsAdmin(null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSubscriptionResponse> getAllUserSubscriptionsAdmin(
            String search,
            UUID planId,
            SubscriptionStatus status,
            Pageable pageable
    ) {
        Specification<UserSubscription> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (planId != null) {
                predicates.add(cb.equal(root.get("subscriptionPlan").get("subscriptionPlanId"), planId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate nameMatch = cb.like(cb.lower(root.get("user").get("fullName")), pattern);
                jakarta.persistence.criteria.Predicate emailMatch = cb.like(cb.lower(root.get("user").get("email")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<UserSubscription> page = userSubscriptionRepository.findAll(spec, pageable);
        return PageResponse.<UserSubscriptionResponse>builder()
                .items(page.getContent().stream().map(sub -> {
                    int owned = sub.getUser() != null ? (int) storeStaffRepository.countByUserUserIdAndStoreRoleName(sub.getUser().getUserId(), "OWNER") : 0;
                    return subscriptionMapper.toUserSubscriptionResponse(sub, owned);
                }).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionStatisticsResponse getSubscriptionStatistics() {
        long totalSubscribedOwners = userSubscriptionRepository.countDistinctUsersWithSubscriptions();
        long activeSubscriptions = userSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long expiredSubscriptions = userSubscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED);
        long pendingSubscriptions = userSubscriptionRepository.countByStatus(SubscriptionStatus.PENDING);
        long cancelledSubscriptions = userSubscriptionRepository.countByStatus(SubscriptionStatus.CANCELLED);

        BigDecimal totalRevenue = subscriptionPaymentRepository.sumTotalRevenue();
        Instant startOfMonth = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);
        Instant startOfToday = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        BigDecimal monthlyRevenue = subscriptionPaymentRepository.sumRevenueSince(startOfMonth);
        BigDecimal todayRevenue = subscriptionPaymentRepository.sumRevenueSince(startOfToday);
        long totalSuccessfulPayments = subscriptionPaymentRepository.countByStatus(SubscriptionPaymentStatus.SUCCESS);

        List<Object[]> userPaymentCounts = subscriptionPaymentRepository.countSuccessfulPaymentsGroupedByUser();
        long newSubs = 0;
        long renewals = 0;
        for (Object[] row : userPaymentCounts) {
            if (row != null && row.length >= 2 && row[1] != null) {
                long count = ((Number) row[1]).longValue();
                if (count >= 1) {
                    newSubs += 1;
                    renewals += (count - 1);
                }
            }
        }

        List<SubscriptionPlan> allPlans = subscriptionPlanRepository.findAllByOrderByPriceAsc();
        List<Object[]> activeSubscribersByPlan = userSubscriptionRepository.countActiveUsersGroupedByPlan();
        Map<UUID, Long> subscriberMap = new HashMap<>();
        for (Object[] row : activeSubscribersByPlan) {
            if (row != null && row.length >= 2 && row[0] != null) {
                subscriberMap.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }

        List<Object[]> revenueByPlan = subscriptionPaymentRepository.sumRevenueGroupedByPlan();
        Map<UUID, BigDecimal> revenueMap = new HashMap<>();
        for (Object[] row : revenueByPlan) {
            if (row != null && row.length >= 2 && row[0] != null) {
                revenueMap.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }

        List<PlanSubscriptionStatsResponse> planStats = allPlans.stream().map(plan -> {
            UUID pid = plan.getSubscriptionPlanId();
            long subs = subscriberMap.getOrDefault(pid, 0L);
            BigDecimal rev = revenueMap.getOrDefault(pid, BigDecimal.ZERO);
            return PlanSubscriptionStatsResponse.builder()
                    .subscriptionPlanId(pid)
                    .name(plan.getName())
                    .displayName(plan.getDisplayName())
                    .price(plan.getPrice())
                    .subscriberCount(subs)
                    .totalRevenue(rev)
                    .build();
        }).toList();

        return SubscriptionStatisticsResponse.builder()
                .totalSubscribedOwners(totalSubscribedOwners)
                .activeSubscriptions(activeSubscriptions)
                .expiredSubscriptions(expiredSubscriptions)
                .pendingSubscriptions(pendingSubscriptions)
                .cancelledSubscriptions(cancelledSubscriptions)
                .newSubscriptionsCount(newSubs)
                .renewalSubscriptionsCount(renewals)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .totalSuccessfulPayments(totalSuccessfulPayments)
                .planStats(planStats)
                .build();
    }
}

