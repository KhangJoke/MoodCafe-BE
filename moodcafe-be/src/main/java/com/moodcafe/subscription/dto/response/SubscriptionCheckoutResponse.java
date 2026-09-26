package com.moodcafe.subscription.dto.response;

import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import com.moodcafe.subscription.entity.enums.SubscriptionPlanCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionCheckoutResponse {

    private UUID paymentId;
    private String transactionCode;
    private SubscriptionPlanCode planCode;
    private String planDisplayName;
    private BigDecimal amount;
    private String paymentMethod;
    private SubscriptionPaymentStatus status;
    private String paymentUrl;
    private String qrCodeUrl;
    private String message;
}
