package com.moodcafe.subscription.dto.request;

import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscribePlanRequest {

    @NotNull(message = "Mã gói dịch vụ không được để trống")
    private SubscriptionPlanCode planCode;

    @Builder.Default
    private Integer durationMonths = 1;

    @Builder.Default
    private String paymentMethod = "VNPAY";
}
