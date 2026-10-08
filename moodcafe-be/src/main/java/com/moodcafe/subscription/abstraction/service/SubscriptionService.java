package com.moodcafe.subscription.abstraction.service;

import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.subscription.dto.request.SubscribePlanRequest;
import com.moodcafe.subscription.dto.response.SubscriptionCheckoutResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPaymentResponse;
import com.moodcafe.subscription.dto.response.SubscriptionPlanResponse;
import com.moodcafe.subscription.dto.response.UserSubscriptionResponse;
import com.moodcafe.subscription.entity.UserSubscription;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SubscriptionService {

    List<SubscriptionPlanResponse> getAvailablePlans();

    UserSubscriptionResponse getCurrentUserSubscription(UUID userId);

    UserSubscription getActiveUserSubscriptionEntity(UUID userId);

    PageResponse<SubscriptionPaymentResponse> getPaymentHistory(UUID userId, Pageable pageable);

    SubscriptionCheckoutResponse subscribePlan(UUID userId, SubscribePlanRequest request);

    UserSubscriptionResponse confirmPayment(String transactionCode);

    void validateBranchLimit(UUID userId);

    List<SubscriptionPlanResponse> getAllPlansAdmin();

    SubscriptionPlanResponse getPlanByIdAdmin(UUID planId);

    SubscriptionPlanResponse createPlan(com.moodcafe.subscription.dto.request.CreateSubscriptionPlanRequest request);

    SubscriptionPlanResponse updatePlan(UUID planId, com.moodcafe.subscription.dto.request.UpdateSubscriptionPlanRequest request);

    SubscriptionPlanResponse togglePlanStatus(UUID planId);

    void deletePlan(UUID planId);

    PageResponse<SubscriptionPaymentResponse> getAllPaymentsAdmin(Pageable pageable);

    PageResponse<SubscriptionPaymentResponse> getAllPaymentsAdmin(
            String search,
            com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus status,
            UUID planId,
            Pageable pageable
    );

    PageResponse<SubscriptionPaymentResponse> getOwnerPaymentHistoryAdmin(UUID ownerUserId, Pageable pageable);

    PageResponse<UserSubscriptionResponse> getAllUserSubscriptionsAdmin(Pageable pageable);

    PageResponse<UserSubscriptionResponse> getAllUserSubscriptionsAdmin(
            String search,
            UUID planId,
            com.moodcafe.subscription.entity.enums.SubscriptionStatus status,
            Pageable pageable
    );

    com.moodcafe.subscription.dto.response.SubscriptionStatisticsResponse getSubscriptionStatistics();

    java.util.List<com.moodcafe.subscription.dto.response.RevenueTrendItemResponse> getRevenueTrends(
            java.time.LocalDate from,
            java.time.LocalDate to,
            String groupBy
    );
}

