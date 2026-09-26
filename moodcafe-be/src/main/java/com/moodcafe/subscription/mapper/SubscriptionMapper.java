package com.moodcafe.subscription.mapper;

import com.moodcafe.subscription.dto.response.SubscriptionPaymentResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPlanResponse;
import com.moodcafe.subscription.dto.response.UserSubscriptionResponse;
import com.moodcafe.subscription.entity.SubscriptionPayment;
import com.moodcafe.subscription.entity.SubscriptionPlan;
import com.moodcafe.subscription.entity.UserSubscription;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class SubscriptionMapper {

    public SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        if (plan == null) return null;

        List<String> features = buildFeaturesList(plan);

        return SubscriptionPlanResponse.builder()
                .subscriptionPlanId(plan.getSubscriptionPlanId())
                .name(plan.getName())
                .planCode(plan.getPlanCode())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .durationDays(plan.getDurationDays())
                .maxBranches(plan.getMaxBranches())
                .tableManagement(plan.isTableManagement())
                .advancedAnalytics(plan.isAdvancedAnalytics())
                .depositRules(plan.isDepositRules())
                .allowSponsoredListing(plan.isAllowSponsoredListing())
                .vipHeroBanner(plan.isVipHeroBanner())
                .qrTableMenu(plan.isQrTableMenu())
                .aiRecommendation(plan.isAiRecommendation())
                .monthlyFreeSponsoredCount(plan.getMonthlyFreeSponsoredCount())
                .dedicatedSupport(plan.isDedicatedSupport())
                .features(features)
                .build();
    }

    public UserSubscriptionResponse toUserSubscriptionResponse(UserSubscription sub, int currentBranchCount) {
        if (sub == null) return null;

        SubscriptionPlan plan = sub.getSubscriptionPlan();
        SubscriptionPlanResponse planResponse = toPlanResponse(plan);

        Long remainingDays = null;
        if (sub.getEndDate() != null) {
            long days = Duration.between(Instant.now(), sub.getEndDate()).toDays();
            remainingDays = Math.max(0, days);
        }

        int maxBranches = plan != null ? plan.getMaxBranches() : 1;
        int monthlyFreeQuota = plan != null ? plan.getMonthlyFreeSponsoredCount() : 0;
        int usedFreeQuota = sub.getMonthlyFreeSponsoredUsed() != null ? sub.getMonthlyFreeSponsoredUsed() : 0;
        int remainingFreeQuota = Math.max(0, monthlyFreeQuota - usedFreeQuota);

        return UserSubscriptionResponse.builder()
                .userSubscriptionId(sub.getUserSubscriptionId())
                .userId(sub.getUser() != null ? sub.getUser().getUserId() : null)
                .plan(planResponse)
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .status(sub.getStatus())
                .remainingDays(remainingDays)
                .currentBranchCount(currentBranchCount)
                .maxBranches(maxBranches)
                .monthlyFreeSponsoredUsed(usedFreeQuota)
                .monthlyFreeSponsoredRemaining(remainingFreeQuota)
                .autoRenew(sub.isAutoRenew())
                .build();
    }

    public SubscriptionPaymentResponse toPaymentResponse(SubscriptionPayment payment) {
        if (payment == null) return null;

        String planName = payment.getSubscriptionPlan() != null ? payment.getSubscriptionPlan().getName() : null;
        String planDisplayName = payment.getSubscriptionPlan() != null ? payment.getSubscriptionPlan().getDisplayName() : null;

        return SubscriptionPaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .transactionCode(payment.getTransactionCode())
                .planName(planName)
                .planDisplayName(planDisplayName)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private List<String> buildFeaturesList(SubscriptionPlan plan) {
        List<String> features = new ArrayList<>();
        if (plan.getMaxBranches() == -1) {
            features.add("Không giới hạn số lượng chi nhánh đăng ký");
        } else {
            features.add("Đăng ký tối đa: " + plan.getMaxBranches() + " chi nhánh");
        }

        if (plan.isTableManagement()) {
            features.add("Quản lý sơ đồ bàn trực tuyến & trạng thái bàn thời gian thực");
        } else {
            features.add("Nhận đặt bàn cơ bản (phê duyệt thủ công)");
        }

        if (plan.isAdvancedAnalytics()) {
            features.add("Báo cáo doanh số & Thống kê đặt bàn chuyên sâu");
        } else {
            features.add("Thống kê cơ bản lượt xem quán");
        }

        if (plan.isDepositRules()) {
            features.add("Thiết lập quy tắc đặt cọc trước (Pre-pay / Deposit)");
        }

        if (plan.isAllowSponsoredListing()) {
            features.add("Mở khóa mua Sponsored Listing (Featured, New Opening, Weekend Picks, Trending)");
        } else {
            features.add("Dịch vụ Sponsored Listing: Bị khóa");
        }

        if (plan.isVipHeroBanner()) {
            features.add("Đặt banner quảng bá nổi bật tại trang chủ (VIP Hero Banner)");
        }

        if (plan.isQrTableMenu()) {
            features.add("Menu QR riêng biệt cho từng bàn");
        }

        if (plan.isAiRecommendation()) {
            features.add("Tích hợp AI đề xuất tự động cho thực khách (AI Recommendation)");
        }

        if (plan.getMonthlyFreeSponsoredCount() > 0) {
            features.add("Tặng " + plan.getMonthlyFreeSponsoredCount() + " lượt Sponsored Listing (7 ngày) miễn phí mỗi tháng");
        }

        if (plan.isDedicatedSupport()) {
            features.add("Kỹ sư hỗ trợ vận hành trực tiếp 24/7");
        }

        return features;
    }
}
