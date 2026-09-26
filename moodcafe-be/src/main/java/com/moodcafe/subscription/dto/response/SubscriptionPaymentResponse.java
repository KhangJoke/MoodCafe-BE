package com.moodcafe.subscription.dto.response;

import com.moodcafe.subscription.entity.enums.SubscriptionPaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPaymentResponse {

    private UUID paymentId;
    private String transactionCode;
    private String planName;
    private String planDisplayName;
    private BigDecimal amount;
    private String paymentMethod;
    private SubscriptionPaymentStatus status;
    private Instant paidAt;
    private String notes;
    private Instant createdAt;
}
