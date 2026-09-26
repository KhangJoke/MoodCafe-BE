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
}
