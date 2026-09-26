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
import com.moodcafe.subscription.dto.request.SubscribePlanRequest;
import com.moodcafe.subscription.dto.response.SubscriptionCheckoutResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPaymentResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPlanResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

        String paymentUrl = buildMockPaymentUrl(transactionCode, totalAmount, paymentMethod);
        payment.setPaymentUrl(paymentUrl);
        payment = subscriptionPaymentRepository.save(payment);

        return SubscriptionCheckoutResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionCode(transactionCode)
                .planCode(plan.getPlanCode())
                .planDisplayName(plan.getDisplayName())
                .amount(totalAmount)
                .paymentMethod(paymentMethod)
                .status(SubscriptionPaymentStatus.PENDING)
                .paymentUrl(paymentUrl)
                .qrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=" + paymentUrl)
                .message("Tạo đơn đăng ký gói thành công, vui lòng tiến hành thanh toán")
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
}
